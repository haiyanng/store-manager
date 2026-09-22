PHASE 5B CUSTOMER ORDER HISTORY MANUAL CHECK

1. Start StoreManagerFX and store-api against the same database.
2. In CustomerShopFX, register or log in as a customer.
3. Add an active product to cart and place an order.
4. In StoreManagerFX Online Order screen, open that order.
5. Apply CONFIRM with a note.
6. Apply one terminal workflow with a note:
   - CANCEL from a cancellable status, or
   - REJECT from a pending/confirmed/preparing/ready status, or
   - REFUND after DELIVERED/COMPLETED.
7. Reopen CustomerShopFX Orders.
8. Select the order detail.
9. Verify the History section displays each status transition with:
   - timestamp
   - old status -> new status
   - note text

Expected result:
Customer can see why an order was cancelled, rejected, or refunded, and can follow the workflow timeline.
