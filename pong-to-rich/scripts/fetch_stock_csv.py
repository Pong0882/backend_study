"""
주식 일봉 데이터 CSV 추출 스크립트 (pykrx)

용도: KRX 10개 종목의 과거 일봉 데이터를 pykrx로 가져와 CSV로 저장
실행: python fetch_stock_csv.py [--start 20000101] [--end 20251231] [--output-dir ./data]

의존성:
    pip install pykrx pandas
"""

import argparse
import os
import time
import pandas as pd
from pykrx import stock as krx


STOCKS = [
    ("005930", "삼성전자"),
    ("000660", "SK하이닉스"),
    ("005380", "현대차"),
    ("035420", "NAVER"),
    ("051910", "LG화학"),
    ("006400", "삼성SDI"),
    ("035720", "카카오"),
    ("028260", "삼성물산"),
    ("012330", "현대모비스"),
    ("066570", "LG전자"),
]


def parse_args():
    parser = argparse.ArgumentParser(description="주식 일봉 CSV 추출")
    parser.add_argument("--start",      type=str, default="20000101", help="시작일 yyyyMMdd (기본값: 20000101)")
    parser.add_argument("--end",        type=str, default="20251231", help="종료일 yyyyMMdd (기본값: 20251231)")
    parser.add_argument("--output-dir", type=str, default="./data",   help="CSV 저장 폴더 (기본값: ./data)")
    return parser.parse_args()


def fetch_ohlcv(ticker: str, name: str, start: str, end: str, output_dir: str):
    print(f"[조회] {name} ({ticker}) {start} ~ {end}")

    df = krx.get_market_ohlcv_by_date(start, end, ticker)

    if df is None or df.empty:
        print(f"  → 데이터 없음, 스킵")
        return 0

    # pykrx 컬럼: 시가 고가 저가 종가 거래량 (인덱스: 날짜)
    df = df.reset_index()
    df.columns = ["trade_date", "open_price", "high_price", "low_price", "close_price", "volume", "change_rate"] \
        if len(df.columns) == 7 else \
        ["trade_date", "open_price", "high_price", "low_price", "close_price", "volume"]

    # trade_date → YYYY-MM-DD 문자열
    df["trade_date"] = pd.to_datetime(df["trade_date"]).dt.strftime("%Y-%m-%d")
    df["stock_code"] = ticker
    df["stock_name"] = name

    # 필요한 컬럼만
    df = df[["stock_code", "stock_name", "trade_date", "open_price", "high_price", "low_price", "close_price", "volume"]]

    # 거래량 0인 행 제거 (휴장일 등)
    df = df[df["volume"] > 0]

    path = os.path.join(output_dir, f"{ticker}_{name}.csv")
    df.to_csv(path, index=False, encoding="utf-8-sig")

    print(f"  → {len(df)}건 저장 → {path}")
    return len(df)


def main():
    args = parse_args()
    os.makedirs(args.output_dir, exist_ok=True)

    total = 0
    for ticker, name in STOCKS:
        count = fetch_ohlcv(ticker, name, args.start, args.end, args.output_dir)
        total += count
        time.sleep(0.5)  # pykrx 레이트리밋 방지

    print(f"\n[완료] 총 {total}건 / {len(STOCKS)}개 종목 → {args.output_dir}/")


if __name__ == "__main__":
    main()
