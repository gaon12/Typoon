package xyz.gaon.typoon.core.engine

object ConversionConstants {
    val COMMON_KOR_WORDS = setOf(
        "안녕", "안녕하세요", "하세요", "감사", "감사합니다", "합니다", "어디", "가요", "오늘", "내일", "어제",
        "사람", "생각", "시간", "친구", "가족", "학교", "공부", "일상", "날씨", "음식", "커피", "우리", "나라",
        "지금", "이미", "벌써", "진짜", "너무", "많이", "조금", "아직", "정말", "그래요", "아니요", "반가워요",
        "행복", "사랑", "미안", "축하", "좋아요", "싫어요", "알겠어요", "도움", "필요", "문제", "정답", "사실",
        "시작", "끝", "기회", "꿈", "희망", "기쁨", "슬픔", "평화", "자유", "정의", "우정", "약속", "비밀",
        "회사", "병원", "은행", "시장", "가게", "식당", "공원", "바다", "하늘", "구름", "바람", "여름", "겨울",
        "먹다", "마시다", "보다", "듣다", "말하다", "가다", "오다", "있다", "없다", "좋다", "나쁘다", "크다",
        "작다", "많다", "적다", "빠르다", "느리다", "어렵다", "쉽다", "예쁘다", "멋지다", "귀엽다", "깨끗",
        "더럽", "밝다", "어둡다", "무겁", "가볍", "공부", "열심히", "노력", "성공", "실패", "결과", "이유",
        "방법", "질문", "대답", "정보", "뉴스", "잡지", "사진", "음악", "영화", "노래", "게임", "여행",
        "빨강", "파랑", "노랑", "초록", "하양", "검정", "어머니", "아버지", "동생", "언니", "오빠", "형",
        "누나", "아침", "점심", "저녁", "밤", "새벽", "주말", "평일", "휴일", "방학", "여행", "준비", "서울",
        "한국", "미국", "일본", "중국", "영국", "독일", "프랑스", "바나나", "사과", "포도", "수박", "김치",
        "라면", "비빔밥", "불고기", "지하철", "버스", "택시", "기차", "비행기", "안경", "시계", "가방", "지갑",
        "휴대전화", "컴퓨터", "우산", "신발", "양말", "장갑", "모자", "화장실", "거실", "주방", "침대", "창문",
        "문", "책상", "의자", "선생님", "학생", "의사", "간호사", "경찰", "소방관", "작가", "가수", "배우",
        "운동", "축구", "야구", "농구", "수영", "등산", "낚시", "요리", "운전", "노력", "도전", "확인", "회의",
        "테스트", "변환", "기록", "설정", "실행", "오류", "보고서", "저장", "자동", "수동", "입력", "화면",
        "공지", "일정", "마지막", "시작", "인사", "링크", "메일", "하단", "참조", "결과", "문서", "품질",
        "회신", "부탁", "빌드", "패치", "릴리즈", "프로젝트", "업데이트", "클라이언트", "서버", "이어서"
    )

    val COMMON_ENG_WORDS = setOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "it", "for", "not", "on", "with", "he",
        "as", "you", "do", "at", "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
        "or", "an", "will", "my", "one", "all", "would", "there", "their", "what", "so", "up", "out",
        "if", "about", "who", "get", "which", "go", "me", "when", "make", "can", "like", "time", "no",
        "just", "him", "know", "take", "people", "into", "year", "your", "good", "some", "could", "them",
        "see", "other", "than", "then", "now", "look", "only", "come", "its", "over", "think", "also",
        "back", "after", "use", "two", "how", "our", "work", "first", "well", "way", "even", "new",
        "want", "because", "any", "these", "give", "day", "most", "us", "hello", "thank", "where",
        "today", "good", "morning", "night", "sorry", "please", "love", "happy", "welcome", "wait",
        "know", "really", "yes", "true", "fact", "information", "news", "problem", "answer", "question",
        "start", "end", "opportunity", "dream", "hope", "joy", "sorrow", "anger", "fear", "peace",
        "freedom", "justice", "friendship", "promise", "secret", "lie", "truth", "school", "office",
        "hospital", "pharmacy", "bank", "market", "store", "cafe", "restaurant", "theater", "library",
        "park", "mountain", "ocean", "river", "tree", "flower", "grass", "sky", "cloud", "sun", "moon",
        "star", "rain", "snow", "wind", "hot", "cold", "warm", "cool", "pretty", "cute", "big", "small",
        "long", "short", "heavy", "light", "fast", "slow", "near", "far", "expensive", "cheap", "delicious",
        "bad", "fun", "boring", "busy", "tired", "sick", "healthy", "scary", "dangerous", "safe", "easy",
        "hard", "possible", "impossible", "important", "need", "enough", "lack", "quiet", "noisy", "clean",
        "dirty", "bright", "dark", "red", "blue", "yellow", "green", "white", "black", "monday", "tuesday",
        "wednesday", "thursday", "friday", "saturday", "sunday", "january", "february", "march", "april",
        "may", "june", "july", "august", "september", "october", "november", "december", "project", "phoenix",
        "release", "note", "update", "build", "patch", "login", "logout", "input", "output", "file", "name",
        "title", "draft", "app", "link", "test", "message", "feature", "keyboard", "android", "server",
        "client", "convert", "error", "check", "save", "copy", "paste", "sync", "final", "alpha", "beta",
        "version", "report"
    )

    val COMMON_ENG_PATTERNS = setOf(
        "th", "he", "in", "er", "an", "re", "on", "at", "en", "nd", "ou", "ea", "ng", "st", "or", "te",
        "is", "it", "al", "ar", "ti", "se", "ve", "ll", "oo", "ck", "up", "pr", "ro", "ct", "ph", "io",
        "le", "ra", "es", "co", "de", "me", "ag", "ge", "ut", "ld", "ion", "ing", "ent", "ate", "ver",
        "pro", "ject", "date", "lease", "patch", "build", "check", "save", "copy"
    )

    val COMMON_KOR_ENDINGS = setOf(
        '은', '는', '이', '가', '을', '를', '에', '의', '로', '와', '과', '다', '요', '요', '듯'
    )
}
