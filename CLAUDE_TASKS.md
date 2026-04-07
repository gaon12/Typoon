# Claude Code Task Board

## 역할
- UI/UX 및 접근성 품질 담당
- 사용자 체감 이슈 우선 처리

## 담당 이슈
1. `#3` 튜토리얼에서 컨텐츠가 잘림
2. `#22` Quick Settings Tile 접근성 개선
3. `#20` `ConversionResult.confidence` UI 반영 강화
4. `#15` Material 3 Dynamic Color 실적용 (Android 12+)

## Codex 검토 결과 (2026-04-07)
- `#3` `조건부 승인`
  - 확인된 변경: `OnboardingScreen`에서 페이지 `Column`을 `fillMaxSize -> fillMaxWidth`로 변경
  - 메모: 제출 설명에 있던 `heightIn(max = 240.dp)` 변경은 실제 코드에 없음
  - 후속 권장: 작은 화면에서 마지막 CTA 가시성 재점검(수동)
- `#22` `승인`
  - 확인된 변경: QS `subtitle`/`contentDescription` 추가 + 다국어 문자열 반영
  - 보정: `subtitle` API 29+ 가드 및 `stateDescription`(API 30+)는 Codex가 추가 반영
- `#20` `승인`
  - 확인된 변경: 경고 카드에 신뢰도 퍼센트/행동 가이드 문구 추가
- `#15` `승인`
  - 확인된 변경: Android 12+ 동적 색상 실제 적용, 하위 버전 fallback 유지
  - 리스크: 12+에서 브랜드 고정 컬러가 약해질 수 있음(제품 판단 필요)

## 제출 형식 (유지)
- `Issue:` `#번호`
- `Changed files:` 파일 목록
- `Why:` 핵심 변경 이유 3줄 이내
- `Verification:` 실행한 테스트/수동 점검
- `Risk:` 남은 리스크
