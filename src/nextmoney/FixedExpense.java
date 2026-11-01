package nextmoney;

/**
 * 월세, 구독료처럼 매달 나가는 정기 지출 항목 1개를 나타내는 모델 클래스.
 */
public class FixedExpense {
    private String name;
    private long amount;

    /**
     * 정기 지출 항목을 생성한다.
     *
     * @param name   항목 이름(예: 월세)
     * @param amount 월 금액(원, 0 이상)
     */
    public FixedExpense(String name, long amount) {
        this.name = name;
        this.amount = amount;
    }

    /**
     * 항목 이름 값을 반환한다.
     * @return 항목 이름
     */
    public String getName() { return name; }
    /**
     * 값을 설정한다.
     * @param name 항목 이름
     */
    public void setName(String name) { this.name = name; }
    /**
     * 월 금액(원) 값을 반환한다.
     * @return 월 금액(원)
     */
    public long getAmount() { return amount; }
    /**
     * 값을 설정한다.
     * @param amount 월 금액(원)
     */
    public void setAmount(long amount) { this.amount = amount; }
}
