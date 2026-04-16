/**
 * 토큰 재발급 API 부하 테스트
 *
 * 목적: DB vs Redis Refresh Token 조회 성능 비교
 *       로그인과 달리 bcrypt가 없어서 순수 저장소 차이가 드러남
 *
 * 시나리오:
 *   1. setup에서 100명 로그인 → refreshToken 수집
 *   2. default에서 수집된 refreshToken으로 재발급 반복
 *
 * 실행:
 *   k6 run test.js
 *
 * 결과 저장:
 *   k6 run test.js 2>&1 | tee RDB-01.txt   (DB 모드)
 *   k6 run test.js 2>&1 | tee Redis-01.txt  (Redis 모드)
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate = new Rate('error_rate');
const refreshDuration = new Trend('refresh_duration', true);

export const options = {
  vus: 50,
  duration: '30s',

  thresholds: {
    http_req_duration: ['p(95)<200'],  // refresh는 bcrypt 없으므로 기준 200ms로 설정
    error_rate: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://192.168.100.10:8080';
const USER_COUNT = 100;
const PASSWORD = 'Test1234!';

// setup: 테스트 시작 전 1회 실행 — 100명 로그인해서 refreshToken 수집
export function setup() {
  const tokens = [];

  for (let i = 1; i <= USER_COUNT; i++) {
    const email = `test${String(i).padStart(3, '0')}@pongtest.com`;
    const res = http.post(
      `${BASE_URL}/api/auth/login`,
      JSON.stringify({ email, password: PASSWORD }),
      { headers: { 'Content-Type': 'application/json' } }
    );

    if (res.status === 200) {
      const body = JSON.parse(res.body);
      tokens.push(body.data.refreshToken);
    }
  }

  console.log(`setup 완료: ${tokens.length}개 refreshToken 수집`);
  return { tokens };
}

// default: setup에서 받은 토큰으로 재발급 반복
export default function (data) {
  const token = data.tokens[Math.floor(Math.random() * data.tokens.length)];

  const res = http.post(
    `${BASE_URL}/api/auth/refresh`,
    JSON.stringify({ refreshToken: token }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  const success = check(res, {
    'status 200': (r) => r.status === 200,
    'has accessToken': (r) => JSON.parse(r.body)?.data?.accessToken !== undefined,
  });

  errorRate.add(!success);
  refreshDuration.add(res.timings.duration);

  sleep(0.1);
}
