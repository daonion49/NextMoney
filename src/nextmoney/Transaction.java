package nextmoney;

import java.time.LocalDate;

/**
 * 수입 또는 지출 거래 1건을 나타내는 모델 클래스.
 *
 * @see AccountBook#registerTransaction(String, String, String, String)
 */
public class Transaction {
    /** 구분 값: 수입. */
    public static final String INCOME = "수입";
    /** 구분 값: 지출. */
    public static final String EXPENSE = "지출";

    private String type;
    private LocalDate date;
    private long amount;
    private String category;

    /**
     * 거래를 생성한다.
     *
     * @param type     구분({@link #INCOME} 또는 {@link #EXPENSE})
     * @param date     거래 날짜
     * @param amount   금액(원, 0 이상)
     * @param category 카테고리 이름
     */
    public Transaction(String type, LocalDate date, long amount, String category) {
        this.type = type;
        this.date = date;
        this.amount = amount;
        this.category = category;
    }

    /**
     * 구분(수입/지출) 값을 반환한다.
     * @return 구분(수입/지출)
     */
    public String getType() { return type; }
    /**
     * 값을 설정한다.
     * @param type 구분(수입/지출)
     */
    public void setType(String type) { this.type = type; }
    /**
     * 거래 날짜 값을 반환한다.
     * @return 거래 날짜
     */
    public LocalDate getDate() { return date; }
    /**
     * 값을 설정한다.
     * @param date 거래 날짜
     */
    public void setDate(LocalDate date) { this.date = date; }
    /**
     * 금액(원) 값을 반환한다.
     * @return 금액(원)
     */
    public long getAmount() { return amount; }
    /**
     * 값을 설정한다.
     * @param amount 금액(원)
     */
    public void setAmount(long amount) { this.amount = amount; }
    /**
     * 카테고리 이름 값을 반환한다.
     * @return 카테고리 이름
     */
    public String getCategory() { return category; }
    /**
     * 값을 설정한다.
     * @param category 카테고리 이름
     */
    public void setCategory(String category) { this.category = category; }

    /**
     * 지출이면 true 값을 반환한다.
     * @return 지출이면 true
     */
    public boolean isExpense() { return EXPENSE.equals(type); }
}
