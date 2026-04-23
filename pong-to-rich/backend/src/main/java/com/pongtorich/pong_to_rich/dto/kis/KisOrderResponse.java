package com.pongtorich.pong_to_rich.dto.kis;

import com.fasterxml.jackson.annotation.JsonProperty;

// KIS 주문 API 응답 (/uapi/domestic-stock/v1/trading/order-cash)
// rt_cd: "0" 성공, 그 외 실패
// output.KRX_FWDG_ORD_ORGNO + output.ODNO 조합이 체결 조회 시 필요한 주문번호
public record KisOrderResponse(
        @JsonProperty("rt_cd") String rtCd,
        @JsonProperty("msg_cd") String msgCd,
        @JsonProperty("msg1") String msg1,
        @JsonProperty("output") Output output
) {
    public record Output(
            @JsonProperty("KRX_FWDG_ORD_ORGNO") String krxOrgNo,
            @JsonProperty("ODNO") String ordNo,
            @JsonProperty("ORD_TMD") String ordTime
    ) {}

    public boolean isSuccess() {
        return "0".equals(rtCd);
    }

    public String kisOrderNo() {
        if (output == null) return null;
        return output.krxOrgNo() + "-" + output.ordNo();
    }
}
