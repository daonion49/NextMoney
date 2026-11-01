package nextmoney;

/**
 * 카테고리 하나의 예산 현황과 경고 단계를 담은 결과 객체.
 *
 * <p>경고 단계: 사용률이 50% 초과이면 1차, 90% 이상이면 2차, 100% 이상이면 3차이다.
 * 예산이 없거나 50% 이하이면 0이다.</p>
 *
 * @see AccountBook#applyBudget(String, String)
 */
public class BudgetStatus {
    private final String category;
    private final long budget;
    private final long spent;
    private final long remaining;
    private final double usageRate;
    private final int warningLevel;

    /**
     * 예산 현황을 생성한다.
     *
     * @param category     카테고리 이름
     * @param budget       월 예산(원, 예산이 없으면 0)
     * @param spent        이번 달 해당 카테고리 지출액(원)
     * @param remaining    남은 예산(원, 초과하면 음수)
     * @param usageRate    사용률(%)
     * @param warningLevel 경고 단계(0~3)
     */
    public BudgetStatus(String category, long budget, long spent, long remaining,
                        double usageRate, int warningLevel) {
        this.category = category;
        this.budget = budget;
        this.spent = spent;
        this.remaining = remaining;
        this.usageRate = usageRate;
        this.warningLevel = warningLevel;
    }

    /**
     * 카테고리 이름 값을 반환한다.
     * @return 카테고리 이름
     */
    public String getCategory() { return category; }
    /**
     * 월 예산(원). 예산이 설정되지 않았으면 0 값을 반환한다.
     * @return 월 예산(원). 예산이 설정되지 않았으면 0
     */
    public long getBudget() { return budget; }
    /**
     * 이번 달 지출액(원) 값을 반환한다.
     * @return 이번 달 지출액(원)
     */
    public long getSpent() { return spent; }
    /**
     * 남은 예산(원). 초과하면 음수 값을 반환한다.
     * @return 남은 예산(원). 초과하면 음수
     */
    public long getRemaining() { return remaining; }
    /**
     * 예산 사용률(%) 값을 반환한다.
     * @return 예산 사용률(%)
     */
    public double getUsageRate() { return usageRate; }
    /**
     * 경고 단계(0~3) 값을 반환한다.
     * @return 경고 단계(0~3)
     */
    public int getWarningLevel() { return warningLevel; }

    /**
     * 예산이 설정되어 있으면 true 값을 반환한다.
     * @return 예산이 설정되어 있으면 true
     */
    public boolean hasBudget() { return budget > 0; }

    /**
     * 경고 단계에 맞는 안내 문구를 만든다.
     *
     * @return 경고 문구. 경고 단계가 0이면 빈 문자열
     */
    public String getWarningMessage() {
        switch (warningLevel) {
            case 1:
                return "[1차 경고] " + category + " 예산의 50%를 넘게 사용했습니다.";
            case 2:
                return "[2차 경고] " + category + " 예산의 90% 이상을 사용했습니다.";
            case 3:
                return "[3차 경고] " + category + " 예산의 100% 이상을 사용했습니다.";
            default:
                return "";
        }
    }
}
