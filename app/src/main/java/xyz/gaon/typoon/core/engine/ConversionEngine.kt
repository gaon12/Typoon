@file:Suppress(
    "CyclomaticComplexMethod",
    "NestedBlockDepth",
    "ReturnCount",
    "MaxLineLength",
    "LoopWithTooManyJumpStatements",
    "TooManyFunctions",
)

package xyz.gaon.typoon.core.engine

data class ConversionResult(
    val resultText: String,
    val direction: ConversionDirection,
    val confidence: Float,
)

enum class ConversionDirection {
    ENG_TO_KOR,
    KOR_TO_ENG,
    UNKNOWN,
}

class ConversionEngine {
    private var exceptions: Set<String> = emptySet()
    private var feedbackPenalty: Float = 0f

    private enum class SegmentScript {
        ENG,
        KOR,
        OTHER,
    }

    private data class TextSegment(
        val text: String,
        val script: SegmentScript,
    )

    fun setExceptions(words: Set<String>) {
        exceptions = words.map { it.trim() }.toSet()
    }

    fun setFeedbackAdjustment(negativeFeedbackRate: Float) {
        // 최근 부정 피드백 비율에 따라 기본 신뢰도를 하향 조정 (0.0 ~ 0.3 범위)
        feedbackPenalty = (negativeFeedbackRate * 0.3f).coerceIn(0f, 0.3f)
    }

    companion object {
        private val ENG_TO_KOR_MAP =
            mapOf(
                'q' to "ㅂ",
                'w' to "ㅈ",
                'e' to "ㄷ",
                'r' to "ㄱ",
                't' to "ㅅ",
                'y' to "ㅛ",
                'u' to "ㅕ",
                'i' to "ㅑ",
                'o' to "ㅐ",
                'p' to "ㅔ",
                'a' to "ㅁ",
                's' to "ㄴ",
                'd' to "ㅇ",
                'f' to "ㄹ",
                'g' to "ㅎ",
                'h' to "ㅗ",
                'j' to "ㅓ",
                'k' to "ㅏ",
                'l' to "ㅣ",
                'z' to "ㅋ",
                'x' to "ㅌ",
                'c' to "ㅊ",
                'v' to "ㅍ",
                'b' to "ㅠ",
                'n' to "ㅜ",
                'm' to "ㅡ",
                'Q' to "ㅃ",
                'W' to "ㅉ",
                'E' to "ㄸ",
                'R' to "ㄲ",
                'T' to "ㅆ",
                'O' to "ㅒ",
                'P' to "ㅖ",
            )

        private val KOR_TO_ENG_MAP = ENG_TO_KOR_MAP.entries.associate { it.value[0] to it.key.toString() }

        // 초성 (19개)
        private val CHOSUNG =
            listOf(
                "ㄱ",
                "ㄲ",
                "ㄴ",
                "ㄷ",
                "ㄸ",
                "ㄹ",
                "ㅁ",
                "ㅂ",
                "ㅃ",
                "ㅅ",
                "ㅆ",
                "ㅇ",
                "ㅈ",
                "ㅉ",
                "ㅊ",
                "ㅋ",
                "ㅌ",
                "ㅍ",
                "ㅎ",
            )

        // 중성 (21개)
        private val JUNGSUNG =
            listOf(
                "ㅏ",
                "ㅐ",
                "ㅑ",
                "ㅒ",
                "ㅓ",
                "ㅔ",
                "ㅕ",
                "ㅖ",
                "ㅗ",
                "ㅘ",
                "ㅙ",
                "ㅚ",
                "ㅛ",
                "ㅜ",
                "ㅝ",
                "ㅞ",
                "ㅟ",
                "ㅠ",
                "ㅡ",
                "ㅢ",
                "ㅣ",
            )

        // 종성 (28개, 0번은 없음)
        private val JONGSUNG =
            listOf(
                "",
                "ㄱ",
                "ㄲ",
                "ㄳ",
                "ㄴ",
                "ㄵ",
                "ㄶ",
                "ㄷ",
                "ㄹ",
                "ㄺ",
                "ㄻ",
                "ㄼ",
                "ㄽ",
                "ㄾ",
                "ㄿ",
                "ㅀ",
                "ㅁ",
                "ㅂ",
                "ㅄ",
                "ㅅ",
                "ㅆ",
                "ㅇ",
                "ㅈ",
                "ㅊ",
                "ㅋ",
                "ㅌ",
                "ㅍ",
                "ㅎ",
            )

        // 복합 모음 조합
        private val COMPLEX_VOWELS =
            mapOf(
                "ㅗㅏ" to "ㅘ",
                "ㅗㅐ" to "ㅙ",
                "ㅗㅣ" to "ㅚ",
                "ㅜㅓ" to "ㅝ",
                "ㅜㅔ" to "ㅞ",
                "ㅜㅣ" to "ㅟ",
                "ㅡㅣ" to "ㅢ",
            )

        // 복합 자음 조합 (종성용)
        private val COMPLEX_CONSONANTS =
            mapOf(
                "ㄱㅅ" to "ㄳ",
                "ㄴㅈ" to "ㄵ",
                "ㄴㅎ" to "ㄶ",
                "ㄹㄱ" to "ㄺ",
                "ㄹㅁ" to "ㄻ",
                "ㄹㅂ" to "ㄼ",
                "ㄹㅅ" to "ㄽ",
                "ㄹㅌ" to "ㄾ",
                "ㄹㅍ" to "ㄿ",
                "ㄹㅎ" to "ㅀ",
                "ㅂㅅ" to "ㅄ",
            )

        private val COMMON_KOR_WORDS =
            setOf(
                "안녕",
                "안녕하세요",
                "하세요",
                "감사",
                "감사합니다",
                "합니다",
                "어디",
                "가요",
                "오늘",
                "내일",
                "어제",
                "사람",
                "생각",
                "시간",
                "친구",
                "가족",
                "학교",
                "공부",
                "일상",
                "날씨",
                "음식",
                "커피",
                "우리",
                "나라",
                "지금",
                "이미",
                "벌써",
                "진짜",
                "너무",
                "많이",
                "조금",
                "아직",
                "정말",
                "그래요",
                "아니요",
                "반가워요",
                "행복",
                "사랑",
                "미안",
                "축하",
                "좋아요",
                "싫어요",
                "알겠어요",
                "도움",
                "필요",
                "문제",
                "정답",
                "사실",
                "시작",
                "끝",
                "기회",
                "꿈",
                "희망",
                "기쁨",
                "슬픔",
                "평화",
                "자유",
                "정의",
                "우정",
                "약속",
                "비밀",
                "회사",
                "병원",
                "은행",
                "시장",
                "가게",
                "식당",
                "공원",
                "바다",
                "하늘",
                "구름",
                "바람",
                "여름",
                "겨울",
                "먹다",
                "마시다",
                "보다",
                "듣다",
                "말하다",
                "가다",
                "오다",
                "있다",
                "없다",
                "좋다",
                "나쁘다",
                "크다",
                "작다",
                "많다",
                "적다",
                "빠르다",
                "느리다",
                "어렵다",
                "쉽다",
                "예쁘다",
                "멋지다",
                "귀엽다",
                "깨끗",
                "더럽",
                "밝다",
                "어둡다",
                "무겁",
                "가볍",
                "공부",
                "열심히",
                "노력",
                "성공",
                "실패",
                "결과",
                "이유",
                "방법",
                "질문",
                "대답",
                "정보",
                "뉴스",
                "잡지",
                "사진",
                "음악",
                "영화",
                "노래",
                "게임",
                "여행",
                "빨강",
                "파랑",
                "노랑",
                "초록",
                "하양",
                "검정",
                "어머니",
                "아버지",
                "동생",
                "언니",
                "오빠",
                "형",
                "누나",
                "아침",
                "점심",
                "저녁",
                "밤",
                "새벽",
                "주말",
                "평일",
                "휴일",
                "방학",
                "여행",
                "준비",
                "서울",
                "한국",
                "미국",
                "일본",
                "중국",
                "영국",
                "독일",
                "프랑스",
                "바나나",
                "사과",
                "포도",
                "수박",
                "김치",
                "라면",
                "비빔밥",
                "불고기",
                "지하철",
                "버스",
                "택시",
                "기차",
                "비행기",
                "안경",
                "시계",
                "가방",
                "지갑",
                "휴대전화",
                "컴퓨터",
                "우산",
                "신발",
                "양말",
                "장갑",
                "모자",
                "화장실",
                "거실",
                "주방",
                "침대",
                "창문",
                "문",
                "책상",
                "의자",
                "선생님",
                "학생",
                "의사",
                "간호사",
                "경찰",
                "소방관",
                "작가",
                "가수",
                "배우",
                "운동",
                "축구",
                "야구",
                "농구",
                "수영",
                "등산",
                "낚시",
                "요리",
                "운전",
                "노력",
                "도전",
                "확인",
                "회의",
                "테스트",
                "변환",
                "기록",
                "설정",
                "실행",
                "오류",
                "보고서",
                "저장",
                "자동",
                "수동",
                "입력",
                "화면",
                "공지",
                "일정",
                "마지막",
                "시작",
                "인사",
                "링크",
                "메일",
                "하단",
                "참조",
                "결과",
                "문서",
                "품질",
                "회신",
                "부탁",
                "빌드",
                "패치",
                "릴리즈",
                "프로젝트",
                "업데이트",
                "클라이언트",
                "서버",
                "이어서",
            )

        private val COMMON_ENG_WORDS =
            setOf(
                "the",
                "be",
                "to",
                "of",
                "and",
                "a",
                "in",
                "that",
                "have",
                "it",
                "for",
                "not",
                "on",
                "with",
                "he",
                "as",
                "you",
                "do",
                "at",
                "this",
                "but",
                "his",
                "by",
                "from",
                "they",
                "we",
                "say",
                "her",
                "she",
                "or",
                "an",
                "will",
                "my",
                "one",
                "all",
                "would",
                "there",
                "their",
                "what",
                "so",
                "up",
                "out",
                "if",
                "about",
                "who",
                "get",
                "which",
                "go",
                "me",
                "when",
                "make",
                "can",
                "like",
                "time",
                "no",
                "just",
                "him",
                "know",
                "take",
                "people",
                "into",
                "year",
                "your",
                "good",
                "some",
                "could",
                "them",
                "see",
                "other",
                "than",
                "then",
                "now",
                "look",
                "only",
                "come",
                "its",
                "over",
                "think",
                "also",
                "back",
                "after",
                "use",
                "two",
                "how",
                "our",
                "work",
                "first",
                "well",
                "way",
                "even",
                "new",
                "want",
                "because",
                "any",
                "these",
                "give",
                "day",
                "most",
                "us",
                "hello",
                "thank",
                "where",
                "today",
                "good",
                "morning",
                "night",
                "sorry",
                "please",
                "love",
                "happy",
                "welcome",
                "wait",
                "know",
                "really",
                "yes",
                "true",
                "fact",
                "information",
                "news",
                "problem",
                "answer",
                "question",
                "start",
                "end",
                "opportunity",
                "dream",
                "hope",
                "joy",
                "sorrow",
                "anger",
                "fear",
                "peace",
                "freedom",
                "justice",
                "friendship",
                "promise",
                "secret",
                "lie",
                "truth",
                "school",
                "office",
                "hospital",
                "pharmacy",
                "bank",
                "market",
                "store",
                "cafe",
                "restaurant",
                "theater",
                "library",
                "park",
                "mountain",
                "ocean",
                "river",
                "tree",
                "flower",
                "grass",
                "sky",
                "cloud",
                "sun",
                "moon",
                "star",
                "rain",
                "snow",
                "wind",
                "hot",
                "cold",
                "warm",
                "cool",
                "pretty",
                "cute",
                "big",
                "small",
                "long",
                "short",
                "heavy",
                "light",
                "fast",
                "slow",
                "near",
                "far",
                "expensive",
                "cheap",
                "delicious",
                "bad",
                "fun",
                "boring",
                "busy",
                "tired",
                "sick",
                "healthy",
                "scary",
                "dangerous",
                "safe",
                "easy",
                "hard",
                "possible",
                "impossible",
                "important",
                "need",
                "enough",
                "lack",
                "quiet",
                "noisy",
                "clean",
                "dirty",
                "bright",
                "dark",
                "red",
                "blue",
                "yellow",
                "green",
                "white",
                "black",
                "monday",
                "tuesday",
                "wednesday",
                "thursday",
                "friday",
                "saturday",
                "sunday",
                "january",
                "february",
                "march",
                "april",
                "may",
                "june",
                "july",
                "august",
                "september",
                "october",
                "november",
                "december",
                "project",
                "phoenix",
                "release",
                "note",
                "update",
                "build",
                "patch",
                "login",
                "logout",
                "input",
                "output",
                "file",
                "name",
                "title",
                "draft",
                "app",
                "link",
                "test",
                "message",
                "feature",
                "keyboard",
                "android",
                "server",
                "client",
                "convert",
                "error",
                "check",
                "save",
                "copy",
                "paste",
                "sync",
                "final",
                "alpha",
                "beta",
                "version",
                "report",
            )

        private val COMMON_ENG_PATTERNS =
            setOf(
                "th",
                "he",
                "in",
                "er",
                "an",
                "re",
                "on",
                "at",
                "en",
                "nd",
                "ou",
                "ea",
                "ng",
                "st",
                "or",
                "te",
                "is",
                "it",
                "al",
                "ar",
                "ti",
                "se",
                "ve",
                "ll",
                "oo",
                "ck",
                "up",
                "pr",
                "ro",
                "ct",
                "ph",
                "io",
                "le",
                "ra",
                "es",
                "co",
                "de",
                "me",
                "ag",
                "ge",
                "ut",
                "ld",
                "ion",
                "ing",
                "ent",
                "ate",
                "ver",
                "pro",
                "ject",
                "date",
                "lease",
                "patch",
                "build",
                "check",
                "save",
                "copy",
            )

        private val COMMON_KOR_ENDINGS =
            setOf(
                '은',
                '는',
                '이',
                '가',
                '을',
                '를',
                '에',
                '의',
                '로',
                '와',
                '과',
                '다',
                '요',
                '요',
                '듯',
            )
    }

    fun convert(input: String): ConversionResult {
        if (input.isBlank()) return ConversionResult(input, ConversionDirection.UNKNOWN, 0.0f)

        val hasEng = input.any { it.isEnglishLetter() }
        val hasKor = input.any { it.isKoreanLetter() }
        if (hasEng && hasKor) {
            return convertMixedInput(input)
        }

        val direction = inferDirection(input)
        val result = convertForced(input, direction)

        val words = result.resultText.split(Regex("\\s+")).filter { it.length >= 2 }
        if (words.isNotEmpty()) {
            val commonSet = if (direction == ConversionDirection.ENG_TO_KOR) COMMON_KOR_WORDS else COMMON_ENG_WORDS
            val matchCount = words.count { it in commonSet }
            if (matchCount.toFloat() / words.size >= 0.5f) {
                return result.copy(confidence = (result.confidence + 0.1f).coerceAtMost(1.0f))
            }
        }

        return result
    }

    private fun convertMixedInput(input: String): ConversionResult {
        val segments = splitIntoScriptSegments(input)
        val output = StringBuilder()
        var engToKorCount = 0
        var korToEngCount = 0

        segments.forEach { segment ->
            when (segment.script) {
                SegmentScript.ENG -> {
                    val converted = convertEnglishSegmentSmart(segment.text)
                    if (converted != segment.text) {
                        engToKorCount++
                    }
                    output.append(converted)
                }

                SegmentScript.KOR -> {
                    val converted = convertKoreanSegmentSmart(segment.text)
                    if (converted != segment.text) {
                        korToEngCount++
                    }
                    output.append(converted)
                }

                SegmentScript.OTHER -> output.append(segment.text)
            }
        }

        val direction =
            when {
                engToKorCount > 0 && korToEngCount == 0 -> ConversionDirection.ENG_TO_KOR
                korToEngCount > 0 && engToKorCount == 0 -> ConversionDirection.KOR_TO_ENG
                engToKorCount > korToEngCount -> ConversionDirection.ENG_TO_KOR
                korToEngCount > engToKorCount -> ConversionDirection.KOR_TO_ENG
                else -> ConversionDirection.UNKNOWN
            }
        val conversionCount = engToKorCount + korToEngCount
        val confidence =
            when {
                conversionCount == 0 -> 0.25f
                direction == ConversionDirection.UNKNOWN -> 0.58f
                else -> 0.74f
            }

        return ConversionResult(
            resultText = output.toString(),
            direction = direction,
            confidence = (confidence - feedbackPenalty * 0.5f).coerceIn(0.1f, 1.0f),
        )
    }

    fun convertForced(
        input: String,
        direction: ConversionDirection,
    ): ConversionResult {
        if (direction == ConversionDirection.UNKNOWN) return ConversionResult(input, direction, 0.0f)

        val confidence = calculateConfidence(input, direction)

        // Split by non-letters (spaces, punctuation, etc) to identify tokens
        val sb = StringBuilder()
        val currentToken = StringBuilder()

        for (char in input) {
            if (char.isLetter()) {
                currentToken.append(char)
            } else {
                if (currentToken.isNotEmpty()) {
                    val token = currentToken.toString()
                    if (token in exceptions) {
                        sb.append(token)
                    } else {
                        sb.append(
                            if (direction ==
                                ConversionDirection.ENG_TO_KOR
                            ) {
                                translateEngToKor(token)
                            } else {
                                translateKorToEng(token)
                            },
                        )
                    }
                    currentToken.clear()
                }
                sb.append(char)
            }
        }

        if (currentToken.isNotEmpty()) {
            val token = currentToken.toString()
            if (token in exceptions) {
                sb.append(token)
            } else {
                sb.append(
                    if (direction ==
                        ConversionDirection.ENG_TO_KOR
                    ) {
                        translateEngToKor(token)
                    } else {
                        translateKorToEng(token)
                    },
                )
            }
        }

        return ConversionResult(sb.toString(), direction, confidence)
    }

    private fun inferDirection(input: String): ConversionDirection {
        val engCount = input.count { it in 'a'..'z' || it in 'A'..'Z' }
        val korCount = input.count { it.code in 0xAC00..0xD7AF || it.code in 0x3131..0x318E }

        val totalLetters = engCount + korCount
        if (totalLetters == 0) return ConversionDirection.UNKNOWN

        val engRatio = engCount.toFloat() / totalLetters
        val korRatio = korCount.toFloat() / totalLetters

        // Check for URL/Email pattern
        val looksLikeUrl =
            input.contains("://") || input.contains("www.") || input.contains(".com") || input.contains("@")

        return when {
            looksLikeUrl -> {
                if (engRatio > 0.9f) {
                    ConversionDirection.UNKNOWN // English URL preserves
                } else if (engCount > korCount) {
                    ConversionDirection.ENG_TO_KOR
                } else {
                    ConversionDirection.KOR_TO_ENG
                }
            }
            engRatio > 0.8f -> ConversionDirection.ENG_TO_KOR
            korRatio > 0.8f -> ConversionDirection.KOR_TO_ENG
            engCount > korCount -> ConversionDirection.ENG_TO_KOR
            korCount > engCount -> ConversionDirection.KOR_TO_ENG
            else -> ConversionDirection.UNKNOWN
        }
    }

    private fun splitIntoScriptSegments(input: String): List<TextSegment> {
        if (input.isEmpty()) return emptyList()

        val segments = mutableListOf<TextSegment>()
        val buffer = StringBuilder()
        var currentScript = charScript(input.first())

        input.forEach { char ->
            val nextScript = charScript(char)
            if (nextScript == currentScript) {
                buffer.append(char)
            } else {
                segments.add(TextSegment(buffer.toString(), currentScript))
                buffer.clear()
                buffer.append(char)
                currentScript = nextScript
            }
        }

        if (buffer.isNotEmpty()) {
            segments.add(TextSegment(buffer.toString(), currentScript))
        }

        return segments
    }

    private fun charScript(char: Char): SegmentScript =
        when {
            char.isEnglishLetter() -> SegmentScript.ENG
            char.isKoreanLetter() -> SegmentScript.KOR
            else -> SegmentScript.OTHER
        }

    private fun convertEnglishSegmentSmart(segment: String): String {
        if (segment in exceptions || shouldPreserveEnglishSegment(segment)) {
            return segment
        }

        val converted = translateEngToKor(segment)
        val originalScore = englishTokenScore(segment)
        val convertedScore = koreanTokenScore(converted)

        return if (convertedScore >= originalScore + 0.08f) converted else segment
    }

    private fun convertKoreanSegmentSmart(segment: String): String {
        if (segment in exceptions || shouldPreserveKoreanSegment(segment)) {
            return segment
        }

        val converted = translateKorToEng(segment)
        val originalScore = koreanTokenScore(segment)
        val convertedScore = englishTokenScore(converted)

        return if (convertedScore >= originalScore + 0.08f) converted else segment
    }

    private fun shouldPreserveEnglishSegment(segment: String): Boolean {
        val lower = segment.lowercase()
        val englishLetterCount = segment.count { it.isEnglishLetter() }
        if (englishLetterCount == 0) return true
        if (looksLikeUrlOrEmail(segment)) return true
        if (lower in COMMON_ENG_WORDS) return true
        if (lower in setOf("a", "i")) return true
        if (segment.length <= 2 && segment.any { it.isUpperCase() }) return true
        if (segment.length <= 4 && segment.all { it.isUpperCase() }) return true
        return false
    }

    private fun shouldPreserveKoreanSegment(segment: String): Boolean {
        if (segment in COMMON_KOR_WORDS) return true

        val syllableCount = segment.count { it.code in 0xAC00..0xD7A3 }
        val jamoCount = segment.count { it.code in 0x3131..0x318E }

        return syllableCount == 1 && jamoCount == 0
    }

    private fun englishTokenScore(token: String): Float {
        if (token.isEmpty()) return 0f

        val lower = token.lowercase()
        if (lower in COMMON_ENG_WORDS) return 1.0f
        if (looksLikeUrlOrEmail(token)) return 1.0f

        val englishLetterCount = token.count { it.isEnglishLetter() }
        if (englishLetterCount == 0) return 0f

        val vowelRatio = token.count { it.lowercaseChar() in "aeiou" }.toFloat() / englishLetterCount
        var score = englishLetterCount.toFloat() / token.length * 0.25f
        if (vowelRatio in 0.25f..0.65f) {
            score += 0.18f
        } else if (vowelRatio == 0f && token.length > 2) {
            score -= 0.25f
        }
        if (token.length in 3..12) score += 0.08f
        score += englishPatternScore(lower)
        score -= englishClusterPenalty(lower)
        if (token.any { it.isUpperCase() }) score += 0.05f
        return score.coerceIn(0f, 0.95f)
    }

    private fun koreanTokenScore(token: String): Float {
        if (token.isEmpty()) return 0f
        if (token in COMMON_KOR_WORDS) return 1.0f

        val syllableCount = token.count { it.code in 0xAC00..0xD7A3 }
        val jamoCount = token.count { it.code in 0x3131..0x318E }
        val koreanCount = syllableCount + jamoCount
        if (koreanCount == 0) return 0f

        var score = koreanCount.toFloat() / token.length * 0.35f
        if (syllableCount > 0) score += 0.3f
        if (token.length in 2..12) score += 0.15f
        if (jamoCount > 0 && syllableCount == 0) score += 0.05f
        if (token.lastOrNull() in COMMON_KOR_ENDINGS) score += 0.08f
        return score.coerceAtMost(0.95f)
    }

    private fun englishPatternScore(lower: String): Float {
        if (lower.length < 2) return 0f

        val hits = COMMON_ENG_PATTERNS.count { pattern -> lower.contains(pattern) }
        return (hits * 0.06f).coerceAtMost(0.24f)
    }

    private fun englishClusterPenalty(lower: String): Float {
        var currentCluster = 0
        var maxCluster = 0

        lower.forEach { char ->
            if (char in 'a'..'z' && char !in "aeiou") {
                currentCluster += 1
                if (currentCluster > maxCluster) {
                    maxCluster = currentCluster
                }
            } else {
                currentCluster = 0
            }
        }

        return when {
            maxCluster >= 5 -> 0.28f
            maxCluster == 4 -> 0.18f
            maxCluster == 3 -> 0.08f
            else -> 0f
        }
    }

    private fun looksLikeUrlOrEmail(input: String): Boolean {
        val lower = input.lowercase()
        return lower.contains("://") || lower.contains("www.") || lower.contains(".com") || lower.contains("@")
    }

    private fun calculateConfidence(
        input: String,
        direction: ConversionDirection,
    ): Float {
        val engCount = input.count { it in 'a'..'z' || it in 'A'..'Z' }
        val korCount = input.count { it.code in 0xAC00..0xD7AF || it.code in 0x3131..0x318E }
        val specialCount = input.count { !it.isLetterOrDigit() && !it.isWhitespace() }

        val totalLetters = engCount + korCount
        if (totalLetters == 0) return 0f

        val initialConfidence =
            when (direction) {
                ConversionDirection.ENG_TO_KOR -> engCount.toFloat() / input.length
                ConversionDirection.KOR_TO_ENG -> korCount.toFloat() / input.length
                else -> 0f
            }

        // Penalize for mixed characters or potential URLs
        var penalty = 0f
        if (input.contains("://") || input.contains("@")) penalty += 0.4f
        if (specialCount > 0) penalty += (specialCount.toFloat() / input.length) * 0.5f

        return (initialConfidence - penalty - feedbackPenalty).coerceIn(0.1f, 1.0f)
    }

    private fun translateEngToKor(input: String): String {
        val sb = StringBuilder()
        val jamos = mutableListOf<String>()

        for (char in input) {
            // Check explicit mapping first (for Q, W, E, R, T, O, P)
            var mapped = ENG_TO_KOR_MAP[char]

            // If not found and it's uppercase, try lowercase
            if (mapped == null && char.isUpperCase()) {
                mapped = ENG_TO_KOR_MAP[char.lowercaseChar()]
            }

            if (mapped != null) {
                jamos.add(mapped)
            } else {
                // If not mapable (space, digit, etc), flush current jamos and add current char
                if (jamos.isNotEmpty()) {
                    sb.append(compose(jamos))
                    jamos.clear()
                }
                sb.append(char)
            }
        }

        if (jamos.isNotEmpty()) {
            sb.append(compose(jamos))
        }

        return sb.toString()
    }

    private fun translateKorToEng(input: String): String {
        val sb = StringBuilder()
        for (char in input) {
            val code = char.code
            if (code in 0xAC00..0xD7A3) { // 완성형 한글
                val base = code - 0xAC00
                val chosungIdx = base / (21 * 28)
                val jungsungIdx = (base % (21 * 28)) / 28
                val jongsungIdx = base % 28

                sb.append(mapJamoToEng(CHOSUNG[chosungIdx]))
                sb.append(mapJamoToEng(JUNGSUNG[jungsungIdx]))
                if (jongsungIdx > 0) {
                    sb.append(mapJamoToEng(JONGSUNG[jongsungIdx]))
                }
            } else if (code in 0x3131..0x318E) { // 자음/모음
                sb.append(mapJamoToEng(char.toString()))
            } else {
                sb.append(char)
            }
        }
        return sb.toString()
    }

    private fun mapJamoToEng(jamo: String): String {
        // 복합 자호 분해
        val decomposed = decomposeJamo(jamo)
        val result = StringBuilder()
        for (j in decomposed) {
            result.append(KOR_TO_ENG_MAP[j] ?: j)
        }
        return result.toString()
    }

    private fun decomposeJamo(jamo: String): List<Char> {
        // 복합 모음/자음 분해 (예: ㅘ -> ㅗ, ㅏ / ㄳ -> ㄱ, ㅅ)
        for ((complex, simple) in COMPLEX_VOWELS) {
            if (jamo == simple) return complex.toList()
        }
        for ((complex, simple) in COMPLEX_CONSONANTS) {
            if (jamo == simple) return complex.toList()
        }
        return listOf(jamo[0])
    }

    private fun compose(jamos: List<String>): String {
        if (jamos.isEmpty()) return ""
        val result = StringBuilder()
        var i = 0

        while (i < jamos.size) {
            // 한 글자 단위 조합 시도
            // 초성 확인
            val cIdx = CHOSUNG.indexOf(jamos[i])
            if (cIdx == -1) {
                // 초성이 아니면 (모음부터 시작하거나 등) 그냥 추가
                result.append(jamos[i])
                i++
                continue
            }

            // 중성 확인
            if (i + 1 >= jamos.size) {
                result.append(jamos[i])
                i++
                continue
            }

            var vIdx = JUNGSUNG.indexOf(jamos[i + 1])
            var vSize = 1

            // 복합 모음 확인 (예: ㅗ + ㅏ = ㅘ)
            if (i + 2 < jamos.size) {
                val combinedV = COMPLEX_VOWELS[jamos[i + 1] + jamos[i + 2]]
                if (combinedV != null) {
                    val nextVIdx = JUNGSUNG.indexOf(combinedV)
                    if (nextVIdx != -1) {
                        vIdx = nextVIdx
                        vSize = 2
                    }
                }
            }

            if (vIdx == -1) {
                result.append(jamos[i])
                i++
                continue
            }

            // 종성 확인
            var tIdx = 0
            var tSize = 0

            if (i + vSize + 1 < jamos.size) {
                // 다음 글자가 자음이고, 그 다음이 모음이 아니면 종성 후보
                val nextChar = jamos[i + vSize + 1]
                val isNextNextVowel = if (i + vSize + 2 < jamos.size) JUNGSUNG.contains(jamos[i + vSize + 2]) else false

                if (CHOSUNG.contains(nextChar) && !isNextNextVowel) {
                    // 종성일 가능성 높음
                    // 복합 자음 종성 확인 (예: ㄱ + ㅅ = ㄳ)
                    if (i + vSize + 2 < jamos.size) {
                        val nextNextChar = jamos[i + vSize + 2]
                        val isNextNextNextVowel =
                            if (i + vSize + 3 <
                                jamos.size
                            ) {
                                JUNGSUNG.contains(jamos[i + vSize + 3])
                            } else {
                                false
                            }

                        val combinedC = COMPLEX_CONSONANTS[nextChar + nextNextChar]
                        if (combinedC != null && !isNextNextNextVowel) {
                            tIdx = JONGSUNG.indexOf(combinedC)
                            tSize = 2
                        }
                    }

                    if (tIdx <= 0) {
                        tIdx = JONGSUNG.indexOf(nextChar)
                        if (tIdx != -1) {
                            tSize = 1
                        } else {
                            tIdx = 0
                        }
                    }
                }
            }

            // Unicode Hangul Syllable 조합: 0xAC00 + (초성 * 21 * 28) + (중성 * 28) + 종성
            val combinedCode = 0xAC00 + (cIdx * 21 * 28) + (vIdx * 28) + tIdx
            result.append(combinedCode.toChar())
            i += 1 + vSize + tSize
        }

        return result.toString()
    }

    private fun Char.isEnglishLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'

    private fun Char.isKoreanLetter(): Boolean = this.code in 0xAC00..0xD7AF || this.code in 0x3131..0x318E
}
