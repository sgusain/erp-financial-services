package com.erp.experiments;

/** Same fields as the ERP budget, with plain numbers to keep the demo simple. */
public record Budget(long id, String name, int fiscalYear, double totalAmount, double spentAmount) {

    public double percentUsed() {
        return totalAmount == 0 ? 0 : Math.round(spentAmount * 1000.0 / totalAmount) / 10.0;
    }
}
