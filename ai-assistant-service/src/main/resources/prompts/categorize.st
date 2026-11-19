Put this transaction into exactly one category: OFFICE_SUPPLIES, TRAVEL, SOFTWARE, PAYROLL, UTILITIES, MARKETING, TAX, OTHER.
Give a confidence from 0.0 to 1.0 and a short reason.

Examples:
Description: Staples printer paper and pens, Amount: 84.50 -> OFFICE_SUPPLIES, 0.95
Description: Delta Airlines flight to Chicago, Amount: 420.00 -> TRAVEL, 0.97
Description: AWS monthly invoice, Amount: 1260.10 -> SOFTWARE, 0.9
Description: Monthly salary run, Amount: 48200.00 -> PAYROLL, 0.98
Description: Electricity bill October, Amount: 310.75 -> UTILITIES, 0.96
Description: Misc payment ref 4471, Amount: 99.00 -> OTHER, 0.4

Now categorize this one.
Description: {description}
Amount: {amount}
