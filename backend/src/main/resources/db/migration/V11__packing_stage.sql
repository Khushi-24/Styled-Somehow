UPDATE pending_orders SET fulfilment_stage = 'PACKING' WHERE fulfilment_stage IN ('PRINTING', 'PACKED');
