# Codex Task Board

## 역할
- PM/통합 검증 담당
- 안정성/데이터 무결성/테스트 인프라 우선 처리

## 담당 이슈
1. `#23` 안정성 하드닝
   - [ ] 기록 저장 원자성 보장 (`insert/count/deleteOldest` 트랜잭션화)
   - [ ] `ConversionDirection` 안전 파싱 fallback
   - [ ] `ShareReceiveActivity` / `ProcessTextActivity` 변환 오프로딩 + 로딩 상태
2. `#5` 업데이트 시 데이터 삭제 리스크 검증
   - [ ] 파괴적 마이그레이션 미사용 재확인
   - [ ] 마이그레이션 경로 테스트로 재발 방지
3. `#21` Room Migration instrumented 테스트 구축
   - [ ] `MigrationTestHelper` 기반 업그레이드 테스트 추가
4. `#18` 테스트 커버리지 확장 (초기 범위)
   - [ ] Repository/Sanitizer/CsvExporter/AppPreferences 중 우선순위 영역부터 확장

## 현재 진행
- `IN PROGRESS`: `#23`

## Codex 검증 책임 (Claude/Gemini 산출물 공통)
- [ ] 요구사항 충족 여부 (이슈 완료 조건 기준)
- [ ] 회귀 위험 평가 (기능/성능/접근성/국제화)
- [ ] 테스트 근거 확인 (`testDebugUnitTest` 등)
- [ ] 스타일/정적분석 영향 확인 (lint/detekt/ktlint 영향 범위)
- [ ] 잔여 리스크를 `SHARES.md`에 기록
