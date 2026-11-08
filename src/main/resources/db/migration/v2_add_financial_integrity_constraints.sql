-- Prevent zero or negative financial transactions
ALTER TABLE financial_transactions
    ADD CONSTRAINT chk_transaction_amount_positive
        CHECK (amount_minor_units > 0);

-- Prevent zero or negative ledger entries
ALTER TABLE ledger_entries
    ADD CONSTRAINT chk_ledger_amount_positive
        CHECK (amount_minor_units > 0);