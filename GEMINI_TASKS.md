# Gemini Task Board

## 역할
- 변환 엔진 구조/성능/정확도 개선 담당
- 회귀 없는 리팩토링 + 테스트 확장

## 담당 이슈
1. `#10` ConversionEngine 리팩토링 (책임 분리, suppress 축소)
2. `#11` `indexOf` 기반 조회 성능 개선
3. `#12` 하드코딩 단어 리스트 외부 분리
4. `#13` 복합 자모 엣지 케이스 보완
5. `#14` 붙어있는 영한 토큰 분리 정확도 향상

## Codex 검토 결과 (2026-04-07)
- `#10` `승인`
  - `ConversionEngine` 책임 분리 확인:
    - `ConversionConstants.kt`
    - `KeyboardMapper.kt`
    - `HangulComposer.kt`
    - `LanguageScorer.kt`
- `#11` `승인`
  - `HangulComposer`에 `CHOSUNG/JUNGSUNG/JONGSUNG` 인덱스 맵 도입으로 반복 `indexOf` 제거 확인
- `#12` `승인`
  - 대형 상수 블록을 `ConversionConstants`로 분리 확인
- `#13`, `#14` `수정 후 승인`
  - 초기 제출 테스트 4건이 잘못된 기대값/입력으로 실패
  - Codex 보정:
    - 복합 모음/복합 종성 테스트 입력 및 기대값 교정
    - 혼용 토큰 테스트 케이스 현실화
    - 방향 추론 동률 fallback을 `UNKNOWN`으로 복원

## 현재 검증 상태
- `./gradlew testDebugUnitTest` 통과

## 제출 형식 (유지)
- `Issue:` `#번호`
- `Changed files:` 파일 목록
- `Behavior change:` 이전/이후 차이
- `Verification:` 실행한 테스트 및 결과
- `Perf note:` 성능 관련 변경 시 수치/관찰
