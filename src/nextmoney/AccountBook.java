package nextmoney;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 가계부의 모든 데이터(거래내역, 카테고리별 예산, 정기 지출, 저축 목표)를 보관하고
 * 입력값 검증과 계산을 담당하는 핵심 클래스.
 *
 * <p>화면 클래스(Panel)는 사용자의 입력 문자열을 이 클래스의 함수에 그대로 전달한다.
 * 입력값이 잘못되면 {@code null} 또는 {@code false}를 반환하며, 오류 메시지는
 * 호출한 화면이 표시한다.</p>
 */
public class AccountBook {
    /** 카테고리 선택 목록. */
    public static final String[] CATEGORIES = {
        "식비", "교통비", "주거/통신", "쇼핑", "문화생활", "의료", "급여", "용돈", "기타"
    };
    /** 통계 조회에서 모든 카테고리를 뜻하는 값. */
    public static final String ALL = "전체";

    /** 최근 변동지출 평균에 사용하는 최대 개월 수. */
    private static final int AVG_MONTHS = 3;

    private final List<Transaction> transactions = new ArrayList<Transaction>();
    private final Map<String, Long> budgets = new LinkedHashMap<String, Long>();
    private final List<FixedExpense> fixedExpenses = new ArrayList<FixedExpense>();
    private SavingsGoal goal;

    /** 비어 있는 가계부를 만든다. */
    public AccountBook() {
        // 모든 목록은 빈 상태로 시작한다.
    }

    // ------------------------------------------------------------------ 거래내역 (UC-01)

    /**
     * 거래내역을 검증한 뒤 목록에 추가한다.
     *
     * @param type       구분("수입" 또는 "지출")
     * @param date       날짜 문자열(yyyy-MM-dd)
     * @param amountText 금액 문자열(숫자, 쉼표 허용)
     * @param category   카테고리 선택값
     * @return 등록된 {@link Transaction}. 필수 항목 누락이나 날짜·금액 형식 오류이면 {@code null}
     */
    public Transaction registerTransaction(String type, String date, String amountText, String category) {
        Transaction t = buildTransaction(type, date, amountText, category);
        if (t == null) {
            return null;
        }
        transactions.add(t);
        return t;
    }

    /**
     * 목록의 거래내역을 수정한다. 검증 규칙은
     * {@link #registerTransaction(String, String, String, String)}과 같다.
     *
     * @param row        수정할 행 번호(0부터)
     * @param type       구분("수입" 또는 "지출")
     * @param date       날짜 문자열(yyyy-MM-dd)
     * @param amountText 금액 문자열
     * @param category   카테고리
     * @return 수정된 {@link Transaction}. 행 번호가 잘못되었거나 입력값 오류이면 {@code null}
     */
    public Transaction updateTransaction(int row, String type, String date, String amountText, String category) {
        if (row < 0 || row >= transactions.size()) {
            return null;
        }
        Transaction t = buildTransaction(type, date, amountText, category);
        if (t == null) {
            return null;
        }
        transactions.set(row, t);
        return t;
    }

    /**
     * 목록의 거래내역을 삭제한다.
     *
     * @param row 삭제할 행 번호(0부터)
     * @return 삭제했으면 true, 행 번호가 잘못되었으면 false
     */
    public boolean deleteTransaction(int row) {
        if (row < 0 || row >= transactions.size()) {
            return false;
        }
        transactions.remove(row);
        return true;
    }

    private Transaction buildTransaction(String type, String date, String amountText, String category) {
        if (blank(type) || blank(category)) {
            return null;
        }
        String ty = type.trim();
        if (!Transaction.INCOME.equals(ty) && !Transaction.EXPENSE.equals(ty)) {
            return null;
        }
        LocalDate d = parseDate(date);
        Long amount = parseAmount(amountText);
        if (d == null || amount == null || amount <= 0) {
            return null;
        }
        return new Transaction(ty, d, amount, category.trim());
    }

    // ------------------------------------------------------------------ 예산 (UC-02)

    /**
     * 카테고리 월 예산을 저장하고(예산 문자열이 있을 때), 이번 달 지출 기준 예산 현황과
     * 경고 단계를 계산한다. 지출을 등록한 직후 경고만 검사할 때는 {@code budgetText}에 null을 준다.
     *
     * <p>경고 단계는 사용률이 50% 초과이면 1, 90% 이상이면 2, 100% 이상이면 3이다.
     * 예산이 없는 카테고리는 경고 단계 0을 반환한다. 지출액은 이번 달(오늘이 속한 달)의
     * 해당 카테고리 지출만 합산한다.</p>
     *
     * @param category   카테고리
     * @param budgetText 월 예산 문자열. 저장하지 않고 현황만 볼 때는 null
     * @return {@link BudgetStatus}. 예산이 숫자가 아니거나 0 이하이면 {@code null}
     */
    public BudgetStatus applyBudget(String category, String budgetText) {
        if (blank(category)) {
            return null;
        }
        String cat = category.trim();
        if (budgetText != null) {
            Long b = parseAmount(budgetText);
            if (b == null || b <= 0) {
                return null;
            }
            budgets.put(cat, b);
        }
        return calcBudgetStatus(cat, YearMonth.now());
    }

    private BudgetStatus calcBudgetStatus(String category, YearMonth month) {
        long spent = expenseSum(category, month);
        Long budget = budgets.get(category);
        if (budget == null) {
            return new BudgetStatus(category, 0, spent, 0, 0, 0);
        }
        double rate = spent * 100.0 / budget;
        int level = 0;
        if (rate >= 100) {
            level = 3;
        } else if (rate >= 90) {
            level = 2;
        } else if (rate > 50) {
            level = 1;
        }
        return new BudgetStatus(category, budget, spent, budget - spent, rate, level);
    }

    // ------------------------------------------------------------------ 정기 지출·저축 목표 (UC-03)

    /**
     * 정기 지출 항목 1개를 등록하거나, 저축 목표를 저장하고, 다음 달 예상 고정지출 합계와
     * 달성률을 계산한다. 입력하지 않은 쪽은 빈 문자열로 전달한다.
     *
     * <p>정기 지출은 이름과 금액을 모두 입력해야 하고 금액은 0 이상의 숫자여야 한다.
     * 저축 목표는 이름과 목표 금액(0보다 큼)을 입력해야 하며, 현재 저축액이 비어 있으면 0으로 본다.
     * 달성률 = 현재 저축액 ÷ 목표 금액 × 100이고 목표 금액 이상이면 100이다.
     * 두 입력 중 하나라도 잘못되면 아무것도 저장하지 않는다.</p>
     *
     * @param fixedName       정기 지출 이름
     * @param fixedAmountText 정기 지출 금액 문자열
     * @param goalName        저축 목표 이름
     * @param goalAmountText  저축 목표 금액 문자열
     * @param savedAmountText 현재 저축액 문자열
     * @return {@link FixedGoalSummary}. 금액이 숫자가 아니거나 음수, 목표 금액이 0이면 {@code null}
     */
    public FixedGoalSummary saveFixedAndGoal(String fixedName, String fixedAmountText,
                                             String goalName, String goalAmountText, String savedAmountText) {
        boolean hasFixed = !blank(fixedName) || !blank(fixedAmountText);
        boolean hasGoal = !blank(goalName) || !blank(goalAmountText) || !blank(savedAmountText);

        FixedExpense newFixed = null;
        SavingsGoal newGoal = null;
        if (hasFixed) {
            Long amount = parseAmount(fixedAmountText);
            if (blank(fixedName) || amount == null) {
                return null;
            }
            newFixed = new FixedExpense(fixedName.trim(), amount);
        }
        if (hasGoal) {
            Long target = parseAmount(goalAmountText);
            Long saved = blank(savedAmountText) ? Long.valueOf(0) : parseAmount(savedAmountText);
            if (blank(goalName) || target == null || target <= 0 || saved == null) {
                return null;
            }
            newGoal = new SavingsGoal(goalName.trim(), target, saved);
        }
        if (newFixed != null) {
            fixedExpenses.add(newFixed);
        }
        if (newGoal != null) {
            goal = newGoal;
        }
        return summary();
    }

    /**
     * 정기 지출 항목을 수정한다.
     *
     * @param row        수정할 행 번호(0부터)
     * @param name       새 이름
     * @param amountText 새 금액 문자열
     * @return 수정했으면 true, 행 번호나 입력값이 잘못되었으면 false
     */
    public boolean updateFixedExpense(int row, String name, String amountText) {
        Long amount = parseAmount(amountText);
        if (row < 0 || row >= fixedExpenses.size() || blank(name) || amount == null) {
            return false;
        }
        fixedExpenses.set(row, new FixedExpense(name.trim(), amount));
        return true;
    }

    /**
     * 정기 지출 항목을 삭제한다.
     *
     * @param row 삭제할 행 번호(0부터)
     * @return 삭제했으면 true, 행 번호가 잘못되었으면 false
     */
    public boolean deleteFixedExpense(int row) {
        if (row < 0 || row >= fixedExpenses.size()) {
            return false;
        }
        fixedExpenses.remove(row);
        return true;
    }

    /**
     * 현재 저장된 정기 지출과 저축 목표로 요약을 계산한다.
     *
     * @return 다음 달 예상 고정지출 합계와 달성률
     */
    public FixedGoalSummary summary() {
        return new FixedGoalSummary(fixedTotal(), goal == null ? 0 : goal.getProgress());
    }

    private long fixedTotal() {
        long sum = 0;
        for (FixedExpense f : fixedExpenses) {
            sum += f.getAmount();
        }
        return sum;
    }

    // ------------------------------------------------------------------ 예상 잔액·통계 (UC-04)

    /**
     * 다음 달 예상 잔액을 계산하고, 선택한 월·카테고리의 지출을 집계한다.
     *
     * <p>예상 잔액 = 예상 수입 − 예상 고정지출 − 최근 평균 변동지출 − 저축 목표액.
     * 최근 평균 변동지출은 지출 거래가 있는 가장 최근 3개월의 월 지출 합계 평균이며,
     * 지출 데이터가 없으면 0으로 보고 {@link ForecastResult#isInsufficientData()}가 true가 된다.
     * 저축 목표액은 저축 목표의 목표 금액이며 목표가 없으면 0이다.</p>
     *
     * @param incomeText 다음 달 예상 수입 문자열
     * @param month      통계를 조회할 월(yyyy-MM). 비어 있거나 형식이 잘못되면 이번 달
     * @param category   조회할 카테고리 또는 "전체"
     * @return {@link ForecastResult}. 예상 수입이 숫자가 아니면 {@code null}
     */
    public ForecastResult forecastBalance(String incomeText, String month, String category) {
        Long income = parseAmount(incomeText);
        if (income == null) {
            return null;
        }
        long fixed = fixedTotal();
        long goalAmount = goal == null ? 0 : goal.getTargetAmount();

        // 지출이 있는 달별 합계 -> 최근 3개월 평균
        TreeMap<YearMonth, Long> monthly = new TreeMap<YearMonth, Long>();
        for (Transaction t : transactions) {
            if (t.isExpense()) {
                YearMonth ym = YearMonth.from(t.getDate());
                Long prev = monthly.get(ym);
                monthly.put(ym, (prev == null ? 0 : prev) + t.getAmount());
            }
        }
        long avg = 0;
        boolean insufficient = monthly.isEmpty();
        if (!insufficient) {
            long sum = 0;
            int count = 0;
            for (Long v : monthly.descendingMap().values()) {
                if (count >= AVG_MONTHS) {
                    break;
                }
                sum += v;
                count++;
            }
            avg = Math.round((double) sum / count);
        }
        long expected = income - fixed - avg - goalAmount;

        // 월·카테고리별 지출 집계
        YearMonth ym = parseMonth(month);
        Map<String, Long> sums = new LinkedHashMap<String, Long>();
        boolean all = blank(category) || ALL.equals(category.trim());
        for (Transaction t : transactions) {
            if (!t.isExpense() || !YearMonth.from(t.getDate()).equals(ym)) {
                continue;
            }
            if (!all && !t.getCategory().equals(category.trim())) {
                continue;
            }
            Long prev = sums.get(t.getCategory());
            sums.put(t.getCategory(), (prev == null ? 0 : prev) + t.getAmount());
        }
        return new ForecastResult(income, expected, fixed, avg, goalAmount, insufficient, sums);
    }

    // ------------------------------------------------------------------ 조회·복원 (화면과 DataStore용)

    /**
     * 거래내역 목록(수정 불가, 등록 순서) 값을 반환한다.
     * @return 거래내역 목록(수정 불가, 등록 순서)
     */
    public List<Transaction> getTransactions() { return Collections.unmodifiableList(transactions); }
    /**
     * 카테고리별 월 예산(수정 불가) 값을 반환한다.
     * @return 카테고리별 월 예산(수정 불가)
     */
    public Map<String, Long> getBudgets() { return Collections.unmodifiableMap(budgets); }
    /**
     * 정기 지출 목록(수정 불가) 값을 반환한다.
     * @return 정기 지출 목록(수정 불가)
     */
    public List<FixedExpense> getFixedExpenses() { return Collections.unmodifiableList(fixedExpenses); }
    /**
     * 저축 목표. 없으면 null 값을 반환한다.
     * @return 저축 목표. 없으면 null
     */
    public SavingsGoal getGoal() { return goal; }

    /**
     * 모든 데이터를 지운다(빈 상태로 초기화).
     */
    public void clear() {
        transactions.clear();
        budgets.clear();
        fixedExpenses.clear();
        goal = null;
    }

    /**
     * 파일에서 읽은 데이터로 기존 데이터를 통째로 교체한다.
     *
     * @param newTransactions 거래내역
     * @param newBudgets      카테고리별 월 예산
     * @param newFixed        정기 지출
     * @param newGoal         저축 목표(없으면 null)
     */
    public void restore(List<Transaction> newTransactions, Map<String, Long> newBudgets,
                        List<FixedExpense> newFixed, SavingsGoal newGoal) {
        clear();
        transactions.addAll(newTransactions);
        budgets.putAll(newBudgets);
        fixedExpenses.addAll(newFixed);
        goal = newGoal;
    }

    // ------------------------------------------------------------------ 도우미

    /**
     * 금액을 "1,234,000원" 형식의 문자열로 바꾼다.
     *
     * @param value 금액(원)
     * @return 쉼표와 "원"이 붙은 문자열
     */
    public static String won(long value) {
        return String.format("%,d원", value);
    }

    private long expenseSum(String category, YearMonth month) {
        long sum = 0;
        for (Transaction t : transactions) {
            if (t.isExpense() && t.getCategory().equals(category)
                    && YearMonth.from(t.getDate()).equals(month)) {
                sum += t.getAmount();
            }
        }
        return sum;
    }

    private static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** 숫자(쉼표 허용, 0 이상)를 읽는다. 잘못된 형식이면 null. */
    private static Long parseAmount(String text) {
        if (blank(text)) {
            return null;
        }
        String s = text.trim().replace(",", "");
        if (!s.matches("\\d{1,15}")) {
            return null;
        }
        return Long.valueOf(s);
    }

    private static LocalDate parseDate(String text) {
        if (blank(text)) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static YearMonth parseMonth(String text) {
        if (blank(text)) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(text.trim());
        } catch (DateTimeParseException e) {
            return YearMonth.now();
        }
    }
}
