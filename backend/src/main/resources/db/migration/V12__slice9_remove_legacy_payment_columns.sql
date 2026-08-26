DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM payment_orders po
        LEFT JOIN (
            SELECT payment_order_id, SUM(final_amount_vnd * quantity) AS item_total
            FROM payment_order_items
            GROUP BY payment_order_id
        ) totals ON totals.payment_order_id = po.id
        WHERE COALESCE(totals.item_total, -1) <> po.final_amount_vnd
    ) THEN
        RAISE EXCEPTION 'Cannot remove legacy payment columns: payment item reconciliation failed';
    END IF;
END $$;

ALTER TABLE payment_orders
    DROP COLUMN IF EXISTS course_ids_csv,
    DROP COLUMN IF EXISTS classroom_offering_ids_csv,
    DROP COLUMN IF EXISTS enrollment_id,
    DROP COLUMN IF EXISTS course_titles;
