import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { toast } from 'sonner';
import { adminApi } from '../api/adminApi';
import { AdminButton, AdminTable, fieldClassName, Panel, PaginationBar } from './AdminUI';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '../../app/components/ui/dialog';
import type { PaymentResponse, RefundAttemptResponse } from '../types';
import { formatCurrency, getApiErrorMessage } from '../utils';

export default function PaymentOperations() {
  const client = useQueryClient();
  const [page, setPage] = useState(0);
  const [reviewPage, setReviewPage] = useState(0);
  const [action, setAction] = useState<{ refund?: RefundAttemptResponse; payment?: PaymentResponse } | null>(null);
  const [reference, setReference] = useState('');
  const [error, setError] = useState<string | null>(null);
  const refunds = useQuery({ queryKey: ['admin', 'refund-attention', page], queryFn: () => adminApi.payments.refundAttention({ page, size: 10, sort: 'createdAt,asc' }) });
  const reviews = useQuery({ queryKey: ['admin', 'payments', 'review', reviewPage], queryFn: () => adminApi.payments.list({ reviewOnly: true, page: reviewPage, size: 10, sort: 'createdAt,asc' }) });
  const mutation = useMutation({
    mutationFn: () => action?.refund
      ? adminApi.payments.confirmRefund(action.refund.id, reference.trim())
      : adminApi.payments.refund(action!.payment!.orderId, { paymentId: action!.payment!.id, reason: reference.trim() }),
    onSuccess: () => { toast.success('Đã ghi nhận xử lý hoàn tiền.'); setAction(null); },
    onError: value => setError(getApiErrorMessage(value)),
    onSettled: () => {
      void client.invalidateQueries({ queryKey: ['admin', 'refund-attention'] });
      void client.invalidateQueries({ queryKey: ['admin', 'payments'] });
      void client.invalidateQueries({ queryKey: ['admin', 'orders'] });
    },
  });
  const open = (value: NonNullable<typeof action>) => { setAction(value); setReference(''); setError(null); };
  return <>
    <Panel className="p-4">
      <h2 className="mb-2 font-bold">Tiền nhận muộn hoặc cần đối soát</h2>
      <p className="mb-4 text-sm text-slate-600">Kiểm tra giao dịch và đơn hàng trước khi hoàn tiền.</p>
      <AdminTable rows={reviews.data?.content ?? []} isLoading={reviews.isLoading} error={reviews.isError ? getApiErrorMessage(reviews.error) : null} onRetry={() => reviews.refetch()} columns={[
        { key: 'payment', header: 'Giao dịch', render: value => <Link to={`/admin/payments/${value.id}`}>#{value.id}</Link> },
        { key: 'order', header: 'Đơn hàng', render: value => <Link to={`/admin/orders/${value.orderId}`}>#{value.orderId}</Link> },
        { key: 'amount', header: 'Số tiền', render: value => formatCurrency(value.amount) },
        { key: 'action', header: 'Xử lý', render: value => <AdminButton size="sm" disabled={value.status !== 'SUCCESS'} onClick={() => open({ payment: value })}>Hoàn tiền giao dịch</AdminButton> },
      ]} />
      <PaginationBar page={reviewPage} totalPages={reviews.data?.totalPages ?? 0} totalElements={reviews.data?.totalElements ?? 0} size={10} onPageChange={setReviewPage} onSizeChange={() => {}} />
    </Panel>
    <Panel className="p-4">
      <h2 className="mb-2 font-bold">Yêu cầu hoàn tiền chưa có kết quả chắc chắn</h2>
      <p className="mb-4 text-sm text-slate-600">Đối chiếu sao kê hoặc cổng thanh toán. Chỉ xác nhận khi đã có bằng chứng tiền được hoàn; không gửi lại yêu cầu chuyển tiền.</p>
      <AdminTable rows={refunds.data?.content ?? []} isLoading={refunds.isLoading} error={refunds.isError ? getApiErrorMessage(refunds.error) : null} onRetry={() => refunds.refetch()} columns={[
        { key: 'order', header: 'Đơn hàng', render: value => <Link to={`/admin/orders/${value.orderId}`}>#{value.orderId}</Link> },
        { key: 'request', header: 'Mã đối soát', render: value => <span className="break-all font-mono text-xs">{value.requestId}</span> },
        { key: 'amount', header: 'Số tiền', render: value => formatCurrency(value.amount) },
        { key: 'status', header: 'Trạng thái', render: value => value.status },
        { key: 'action', header: 'Xác nhận', render: value => <AdminButton size="sm" onClick={() => open({ refund: value })}>Đã hoàn theo chứng từ</AdminButton> },
      ]} />
      <PaginationBar page={page} totalPages={refunds.data?.totalPages ?? 0} totalElements={refunds.data?.totalElements ?? 0} size={10} onPageChange={setPage} onSizeChange={() => {}} />
    </Panel>
    <Dialog open={Boolean(action)} onOpenChange={value => { if (!value && !mutation.isPending) setAction(null); }}>
      <DialogContent><DialogHeader><DialogTitle>{action?.refund ? 'Xác nhận tiền đã hoàn' : 'Hoàn toàn bộ giao dịch'}</DialogTitle>
        <DialogDescription>{action?.refund ? 'Nhập mã chứng từ/sao kê đã đối chiếu. Thao tác này ghi nhận kết quả, không gửi tiền lần nữa.' : 'VNPay sẽ nhận yêu cầu hoàn tiền. Với COD, chỉ xác nhận sau khi đã trả tiền cho khách.'}</DialogDescription></DialogHeader>
        <form className="space-y-4" onSubmit={event => { event.preventDefault(); setError(null); mutation.mutate(); }}>
          {error && <p role="alert" className="text-sm text-rose-700">{error}</p>}
          <label className="block text-sm font-medium">{action?.refund ? 'Chứng từ xác nhận' : 'Lý do hoàn tiền'}
            <input className={fieldClassName} required minLength={3} maxLength={1000} value={reference} onChange={event => setReference(event.target.value)} />
          </label>
          <DialogFooter><AdminButton type="submit" disabled={mutation.isPending || reference.trim().length < 3}>{mutation.isPending ? 'Đang xử lý...' : 'Xác nhận'}</AdminButton></DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  </>;
}
