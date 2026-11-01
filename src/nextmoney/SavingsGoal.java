package nextmoney;

/**
 * 저축 목표(이름, 목표 금액, 현재 저축액)를 나타내는 모델 클래스.
 */
public class SavingsGoal {
    private String name;
    private long targetAmount;
    private long savedAmount;

    /**
     * 저축 목표를 생성한다.
     *
     * @param name         목표 이름
     * @param targetAmount 목표 금액(원, 0보다 커야 한다)
     * @param savedAmount  현재 저축액(원, 0 이상)
     */
    public SavingsGoal(String name, long targetAmount, long savedAmount) {
        this.name = name;
        this.targetAmount = targetAmount;
        this.savedAmount = savedAmount;
    }

    /**
     * 목표 이름 값을 반환한다.
     * @return 목표 이름
     */
    public String getName() { return name; }
    /**
     * 목표 금액(원) 값을 반환한다.
     * @return 목표 금액(원)
     */
    public long getTargetAmount() { return targetAmount; }
    /**
     * 현재 저축액(원) 값을 반환한다.
     * @return 현재 저축액(원)
     */
    public long getSavedAmount() { return savedAmount; }

    /**
     * 달성률을 계산한다. 달성률 = 현재 저축액 ÷ 목표 금액 × 100 이며,
     * 목표 금액 이상 모았으면 100으로 제한한다.
     *
     * @return 달성률(0~100 정수). 목표 금액이 0 이하이면 0
     */
    public int getProgress() {
        if (targetAmount <= 0) {
            return 0;
        }
        long p = savedAmount * 100 / targetAmount;
        return (int) Math.max(0, Math.min(100, p));
    }
}
