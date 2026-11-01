/**
 * 넥스트 머니(NextMoney) - 직접 입력한 거래내역, 예산, 정기 지출, 저축 목표를 바탕으로
 * 다음 달 예상 잔액을 계산해 주는 Java Swing 가계부 프로그램.
 *
 * <p>클래스는 세 계층으로 나뉜다.</p>
 * <ul>
 *   <li>화면(UI): {@link nextmoney.MainFrame}, {@link nextmoney.TransactionPanel},
 *       {@link nextmoney.BudgetPanel}, {@link nextmoney.GoalPanel}, {@link nextmoney.StatsPanel}</li>
 *   <li>데이터 관리: {@link nextmoney.AccountBook}(검증·계산), {@link nextmoney.DataStore}(파일 입출력)</li>
 *   <li>모델: {@link nextmoney.Transaction}, {@link nextmoney.FixedExpense}, {@link nextmoney.SavingsGoal}
 *       및 결과 객체 {@link nextmoney.BudgetStatus}, {@link nextmoney.FixedGoalSummary}, {@link nextmoney.ForecastResult}</li>
 * </ul>
 */
package nextmoney;
