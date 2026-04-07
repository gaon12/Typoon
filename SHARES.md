# Shared Notes

## 협업 규칙
- 각 에이전트는 아래 템플릿으로 제출한다.
  - `Issue:`
  - `Changed files:`
  - `Why:`
  - `Verification:`
  - `Risk:`
- Codex는 제출 내용과 실제 diff를 대조해 승인/반려를 기록한다.

## Codex 검토 요약 (2026-04-07)
- Claude Code 변경: `부분 승인`
  - `#22`, `#20`, `#15` 승인
  - `#3` 조건부 승인 (설명과 실제 diff 일부 불일치)
- Gemini 변경: `수정 후 승인`
  - 엔진 분리/상수 분리/O(1) 조회 개선은 유효
  - 테스트 케이스 4건 오류를 Codex가 보정

## Claude Code 검토 상세

### Issue: `#3` 튜토리얼 잘림
- `Changed files:` `feature/onboarding/OnboardingScreen.kt`
- `Review:` 조건부 승인
- `Note:` 제출 설명의 `heightIn(max = 240.dp)`는 실제 코드에 없음. 현재 반영은 `fillMaxSize -> fillMaxWidth`.
- `Action:` 소형 화면에서 CTA/노트 카드 가시성 수동 재검증 필요

### Issue: `#22` QS Tile 접근성
- `Changed files:` `core/tile/ClipboardConvertTileService.kt`, `values/strings.xml`, `values-ko/strings.xml`
- `Review:` 승인
- `Evidence:` `subtitle`/`contentDescription` 및 다국어 문자열 반영 확인

### Issue: `#20` confidence UI
- `Changed files:` `feature/result/ResultScreen.kt`, `values/strings.xml`, `values-ko/strings.xml`
- `Review:` 승인
- `Evidence:` 경고 카드에 confidence 퍼센트 + 행동 가이드 문구 반영 확인

### Issue: `#15` Dynamic Color
- `Changed files:` `ui/theme/Theme.kt`
- `Review:` 승인
- `Evidence:` Android 12+에서 `dynamicDarkColorScheme`/`dynamicLightColorScheme` 적용 확인
- `Risk:` 12+에서 브랜드 고정 컬러 약화 가능성

## Gemini 검토 상세

### Issue: `#10`, `#11`, `#12`
- `Changed files:`
  - `core/engine/ConversionEngine.kt`
  - `core/engine/ConversionConstants.kt`
  - `core/engine/KeyboardMapper.kt`
  - `core/engine/HangulComposer.kt`
  - `core/engine/LanguageScorer.kt`
- `Review:` 승인
- `Evidence:` 엔진 책임 분리, 상수 분리, 인덱스 맵 기반 조회로 반복 `indexOf` 제거 확인

### Issue: `#13`, `#14`
- `Changed files:` `core/engine/ConversionEngine.kt`, `core/engine/ConversionEngineTest.kt`
- `Review:` 수정 후 승인
- `Codex correction:`
  - 잘못된 테스트 입력/기대값 4건 교정
  - 방향 추론 동률 fallback `UNKNOWN` 복원

## Codex 직접 수정 내역
- `#23` 안정성 하드닝
  - `ConversionDirection.fromPersisted` 추가 (안전 파싱)
  - `HistoryRepository.insert` 원자화 (`ConversionDao.insertAndTrim` @Transaction)
  - `ShareReceiveActivity`/`ProcessTextActivity` 변환 오프로딩 + 로딩 UI
  - `ConversionLoadingContent` 및 다국어 로딩 문구 추가
- 테스트
  - `ConversionDirectionTest` 추가
  - `ConversionEngineTest` 보정

## 검증
- 실행: `./gradlew testDebugUnitTest`
- 결과: 통과

## 추가 검토 라운드 (2026-04-07)
- 범위: Claude/Gemini 추가 변경분 재검토
- Findings:
  1. `#22` QS Tile에서 `subtitle`를 API 가드 없이 호출하고 있었음
     - `Tile.setSubtitle()`는 API 29+라서 minSdk 26 환경에서 런타임 크래시 가능
- Codex 조치:
  - `ClipboardConvertTileService`에서 `subtitle`를 API 29+ 분기로 이동
  - API 30+에서 `stateDescription` 추가
  - 다국어 문자열 `qs_tile_state_active` 추가
- 재검증:
  - `./gradlew testDebugUnitTest --tests "xyz.gaon.typoon.core.engine.ConversionEngineTest" --tests "xyz.gaon.typoon.core.engine.ConversionDirectionTest"` 통과

## 남은 확인 필요 항목
1. `#11` 성능 개선 근거
   - 기능 테스트는 통과했지만, 벤치마크/처리시간 수치 근거는 아직 없음
2. `#3` 튜토리얼 잘림 재현 확인
   - 코드상 레이아웃 수정은 반영되었으나, 실제 문제 기기에서 재현 방지 확인 스크린샷이 아직 없음

## 결정 필요 / 질문
1. `#19` 한글 초성 검색 정책
   - 초성 검색을 정식 지원할지, 비지원 + 안내로 갈지 제품 결정 필요
2. `#16` 접근성 서비스 기반 즉시 변환
   - Play 정책/권한 고지/UX 가이드 포함 별도 설계 이슈 권장
