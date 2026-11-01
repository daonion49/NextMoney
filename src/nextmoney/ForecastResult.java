package nextmoney;

import java.util.Collections;
import java.util.Map;

/**
 * 다음 달 예상 잔액 계산 결과와 월·카테고리별 지출 합계를 담은 결과 객체.
 *
 * @see AccountBook#forecastBalance(String, String, String)
 */
public class ForecastResult {
    private final long income;
    private final long expectedBalance;
    private final long shortage;
    private final long fixedTotal;
    private final long avgVariable;
    private final long goalAmount;
    private final boolean insufficientData;
    private final Map<String, Long> categorySums;

    /**
     * 결과를 생성한다.
     *
     * @param income           다음 달 예상 수입(원)
     * @param expectedBalance  다음 달 예상 잔액(원, 음수 가능)
     * @param fixedTotal       다음 달 예상 고정지출(원)
     * @param avgVariable      최근 평균 변동지출(원)
     * @param goalAmount       저축 목표액(원)
     * @param insufficientData 지출 데이터가 없어 평균 변동지출을 0으로 본 경우 true
     * @param categorySums     선택한 월·카테고리의 카테고리별 지출 합계
     */
    public ForecastResult(long income, long expectedBalance, long fixedTotal, long avgVariable,
                          long goalAmount, boolean insufficientData, Map<String, Long> categorySums) {
        this.income = income;
        this.expectedBalance = expectedBalance;
        this.shortage = expectedBalance < 0 ? -expectedBalance : 0;
        this.fixedTotal = fixedTotal;
        this.avgVariable = avgVariable;
        this.goalAmount = goalAmount;
        this.insufficientData = insufficientData;
        this.categorySums = Collections.unmodifiableMap(categorySums);
    }

    /**
     * 다음 달 예상 수입(원) 값을 반환한다.
     * @return 다음 달 예상 수입(원)
     */
    public long getIncome() { return income; }
    /**
     * 다음 달 예상 잔액(원). 부족하면 음수 값을 반환한다.
     * @return 다음 달 예상 잔액(원). 부족하면 음수
     */
    public long getExpectedBalance() { return expectedBalance; }
    /**
     * 예상 잔액이 음수일 때 부족한 금액(원), 아니면 0 값을 반환한다.
     * @return 예상 잔액이 음수일 때 부족한 금액(원), 아니면 0
     */
    public long getShortage() { return shortage; }
    /**
     * 다음 달 예상 고정지출(원) 값을 반환한다.
     * @return 다음 달 예상 고정지출(원)
     */
    public long getFixedTotal() { return fixedTotal; }
    /**
     * 최근 평균 변동지출(원) 값을 반환한다.
     * @return 최근 평균 변동지출(원)
     */
    public long getAvgVariable() { return avgVariable; }
    /**
     * 저축 목표액(원) 값을 반환한다.
     * @return 저축 목표액(원)
     */
    public long getGoalAmount() { return goalAmount; }
    /**
     * 지출 데이터가 부족해 평균 변동지출을 0으로 계산했으면 true 값을 반환한다.
     * @return 지출 데이터가 부족해 평균 변동지출을 0으로 계산했으면 true
     */
    public boolean isInsufficientData() { return insufficientData; }
    /**
     * 카테고리별 지출 합계(수정 불가) 값을 반환한다.
     * @return 카테고리별 지출 합계(수정 불가)
     */
    public Map<String, Long> getCategorySums() { return categorySums; }
}
