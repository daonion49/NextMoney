# NextMoney

넥스트 머니 (NextMoney) - Java Swing 가계부 프로그램
학번 251522 / 강다언

[실행]
  java -jar ssd_ip_cp4_2026f_251522_app.jar
  - JAR 파일은 어느 폴더에 있어도 실행됩니다. (Java 8 이상)
  - 입력한 데이터는 사용자 폴더(예: C:\Users\사용자이름)의 nextmoney_data.txt 에 저장되며,
    [저장] 버튼을 누르거나 프로그램을 종료할 때 저장되고 다음 실행 때 자동으로 불러옵니다.
  - 저장된 데이터를 지우고 처음부터 시작하려면 위 파일을 삭제하면 됩니다.

[소스 실행 - Eclipse]
  File > Import > Existing Projects into Workspace > 이 폴더(nextmoney) 선택
  src/nextmoney/MainFrame.java 를 Run As > Java Application

[소스 실행 - 명령줄]
  javac -encoding UTF-8 -d out src/nextmoney/*.java
  java -cp out nextmoney.MainFrame

[패키지 구성: nextmoney]
  화면   : MainFrame, TransactionPanel, BudgetPanel, GoalPanel, StatsPanel
  데이터 : AccountBook(검증/계산), DataStore(파일 입출력)
  모델   : Transaction, FixedExpense, SavingsGoal
  결과   : BudgetStatus, FixedGoalSummary, ForecastResult
