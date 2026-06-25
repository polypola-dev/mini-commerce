import {
  getProducts,
  createOrder,
  completeFakePayment,
  getReviews,
  createReview,
  deleteReview,
} from '@/lib/api'

// ----------------------------------------------------------------
// global.fetch Mock 설정
// ----------------------------------------------------------------
const mockFetch = jest.fn()
global.fetch = mockFetch

beforeEach(() => {
  mockFetch.mockReset()
})

/** 성공 응답 Mock 헬퍼 */
function mockOk(data: unknown) {
  mockFetch.mockResolvedValueOnce({
    ok: true,
    json: async () => data,
    text: async () => JSON.stringify(data),
  })
}

/** 실패 응답 Mock 헬퍼 */
function mockError(status: number, body = '') {
  mockFetch.mockResolvedValueOnce({
    ok: false,
    status,
    text: async () => body,
  })
}

// ----------------------------------------------------------------
// getProducts
// ----------------------------------------------------------------
describe('getProducts', () => {
  test('검색어 없이 호출 시 /api/products 경로로 fetch', async () => {
    const page = {
      content: [
        { id: '1', name: '상품1', description: '설명', price: 1000, availableStock: 10, imageUrl: '' },
      ],
      totalElements: 1,
      totalPages: 1,
      page: 0,
      size: 20,
    }
    mockOk(page)

    const result = await getProducts()

    const calledUrl: string = mockFetch.mock.calls[0][0]
    const url = new URL(calledUrl)
    expect(url.pathname).toBe('/api/products')
    expect(url.searchParams.has('q')).toBe(false)
    expect(result).toEqual(page)
  })

  test('검색어 포함 호출 시 ?q= 파라미터 추가', async () => {
    mockOk({ content: [], totalElements: 0, totalPages: 0, page: 0, size: 20 })

    await getProducts({ q: '신발' })

    const calledUrl: string = mockFetch.mock.calls[0][0]
    const url = new URL(calledUrl)
    expect(url.searchParams.get('q')).toBe('신발')
  })

  test('응답 실패(ok=false) 시 에러 throw', async () => {
    mockError(500)

    await expect(getProducts()).rejects.toThrow('Failed to fetch products')
  })
})

// ----------------------------------------------------------------
// createOrder
// ----------------------------------------------------------------
describe('createOrder', () => {
  test('POST /api/proxy/orders 로 요청 전송', async () => {
    const orderResp = { orderId: 'order-1', status: 'PENDING', totalAmount: 5000 }
    mockOk(orderResp)

    const result = await createOrder({ items: [{ productId: 'prod-1', quantity: 1 }] })

    expect(mockFetch).toHaveBeenCalledWith(
      '/api/proxy/orders',
      expect.objectContaining({
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ items: [{ productId: 'prod-1', quantity: 1 }] }),
      }),
    )
    expect(result).toEqual(orderResp)
  })

  test('응답 실패 시 응답 본문 메시지로 에러 throw', async () => {
    mockError(409, '재고가 부족합니다')

    await expect(
      createOrder({ items: [{ productId: 'prod-1', quantity: 999 }] }),
    ).rejects.toThrow('재고가 부족합니다')
  })

  test('응답 실패이고 본문이 비어 있으면 기본 메시지로 에러 throw', async () => {
    mockError(500, '')

    await expect(createOrder({ items: [] })).rejects.toThrow('Failed to create order')
  })
})

// ----------------------------------------------------------------
// completeFakePayment
// ----------------------------------------------------------------
describe('completeFakePayment', () => {
  test('POST /api/proxy/orders/{orderId}/complete-payment 로 요청 전송', async () => {
    const orderResp = { orderId: 'order-1', status: 'PAID', totalAmount: 5000 }
    mockOk(orderResp)

    const result = await completeFakePayment('order-1')

    expect(mockFetch).toHaveBeenCalledWith(
      '/api/proxy/orders/order-1/complete-payment',
      expect.objectContaining({ method: 'POST' }),
    )
    expect(result).toEqual(orderResp)
  })

  test('응답 실패 시 에러 throw', async () => {
    mockError(500, '결제 처리 실패')

    await expect(completeFakePayment('order-1')).rejects.toThrow('결제 처리 실패')
  })
})

// ----------------------------------------------------------------
// getReviews
// ----------------------------------------------------------------
describe('getReviews', () => {
  test('GET /api/proxy/products/{productId}/reviews 로 요청 전송', async () => {
    const reviewResp = { reviews: [], averageRating: 0, totalCount: 0 }
    mockOk(reviewResp)

    const result = await getReviews('prod-1')

    expect(mockFetch).toHaveBeenCalledWith(
      '/api/proxy/products/prod-1/reviews',
      expect.any(Object),
    )
    expect(result).toEqual(reviewResp)
  })

  test('응답 실패 시 에러 throw', async () => {
    mockError(500)

    await expect(getReviews('prod-1')).rejects.toThrow('Failed to fetch reviews')
  })
})

// ----------------------------------------------------------------
// createReview
// ----------------------------------------------------------------
describe('createReview', () => {
  test('POST /api/proxy/reviews 로 요청 전송', async () => {
    const review = {
      id: 'rev-1',
      productId: 'prod-1',
      authorId: 'user-1',
      rating: 5,
      content: '매우 좋아요',
      createdAt: '2024-01-01T00:00:00Z',
    }
    mockOk(review)

    const result = await createReview({ productId: 'prod-1', rating: 5, content: '매우 좋아요' })

    expect(mockFetch).toHaveBeenCalledWith(
      '/api/proxy/reviews',
      expect.objectContaining({
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ productId: 'prod-1', rating: 5, content: '매우 좋아요' }),
      }),
    )
    expect(result).toEqual(review)
  })

  test('응답 실패 시 에러 throw', async () => {
    mockError(400, '잘못된 요청입니다')

    await expect(
      createReview({ productId: 'prod-1', rating: 6, content: '' }),
    ).rejects.toThrow('잘못된 요청입니다')
  })
})

// ----------------------------------------------------------------
// deleteReview
// ----------------------------------------------------------------
describe('deleteReview', () => {
  test('DELETE /api/proxy/reviews/{reviewId} 로 요청 전송', async () => {
    mockFetch.mockResolvedValueOnce({ ok: true })

    await deleteReview('rev-1')

    expect(mockFetch).toHaveBeenCalledWith(
      '/api/proxy/reviews/rev-1',
      expect.objectContaining({ method: 'DELETE' }),
    )
  })

  test('응답 실패 시 에러 throw', async () => {
    mockError(404, '리뷰를 찾을 수 없습니다')

    await expect(deleteReview('rev-1')).rejects.toThrow('리뷰를 찾을 수 없습니다')
  })
})
