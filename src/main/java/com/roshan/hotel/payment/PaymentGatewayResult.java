package com.roshan.hotel.payment;

public class PaymentGatewayResult {

    private final PaymentGatewayStatus status;
    private final String providerPaymentId;

    private PaymentGatewayResult(
            PaymentGatewayStatus status,
            String providerPaymentId
    ) {
        this.status = status;
        this.providerPaymentId = providerPaymentId;
    }

    public static PaymentGatewayResult success(String providerPaymentId) {
        return new PaymentGatewayResult(
                PaymentGatewayStatus.SUCCESS,
                providerPaymentId
        );
    }

    public static PaymentGatewayResult failed() {
        return new PaymentGatewayResult(
                PaymentGatewayStatus.FAILED,
                null
        );
    }

    public static PaymentGatewayResult unknown() {
        return new PaymentGatewayResult(
                PaymentGatewayStatus.UNKNOWN,
                null
        );
    }

    public PaymentGatewayStatus getStatus() {
        return status;
    }

    public String getProviderPaymentId() {
        return providerPaymentId;
    }
}
