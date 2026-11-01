package nextmoney;

/**
 * 다음 달 예상 고정지출 합계와 저축 목표 달성률을 담은 결과 객체.
 *
 * @see AccountBook#saveFixedAndGoal(String, String, String, String, String)
 */
public class FixedGoalSummary {
    private final long fixedTotal;
    private final int progress;

    /**
     * 요약을 생성한다.
     *
     * @param fixedTotal 정기 지출 합계(원)
     * @param progress   저축 목표 달성률(0~100). 목표가 없으면 0
     */
    public FixedGoalSummary(long fixedTotal, int progress) {
        this.fixedTotal = fixedTotal;
        this.progress = progress;
    }

    /**
     * 다음 달 예상 고정지출(정기 지출 합계, 원) 값을 반환한다.
     * @return 다음 달 예상 고정지출(정기 지출 합계, 원)
     */
    public long getFixedTotal() { return fixedTotal; }
    /**
     * 저축 목표 달성률(0~100) 값을 반환한다.
     * @return 저축 목표 달성률(0~100)
     */
    public int getProgress() { return progress; }
}
