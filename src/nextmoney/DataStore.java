package nextmoney;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link AccountBook}의 데이터를 텍스트 파일(UTF-8, 탭 구분)에 저장하고 불러오는 클래스.
 *
 * <p>파일의 각 줄은 {@code T}(거래내역), {@code B}(예산), {@code F}(정기 지출), {@code G}(저축 목표)
 * 중 하나로 시작한다. 첫 줄은 파일 형식을 나타내는 머리글이다.</p>
 */
public class DataStore {
    private static final String HEADER = "NEXTMONEY\t1";

    private final AccountBook accountBook;
    private String lastError = "";

    /**
     * 데이터 저장소를 생성한다.
     *
     * @param accountBook 저장하고 복원할 가계부
     */
    public DataStore(AccountBook accountBook) {
        this.accountBook = accountBook;
    }

    /**
     * 거래내역, 카테고리별 예산, 정기 지출, 저축 목표를 파일에 기록한다.
     *
     * @param filePath 저장할 파일 경로
     * @return 저장에 성공하면 true, 파일 쓰기에 실패하면 false
     */
    public boolean saveData(String filePath) {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), "UTF-8"));
            out.write(HEADER);
            out.newLine();
            for (Transaction t : accountBook.getTransactions()) {
                out.write("T\t" + t.getType() + "\t" + t.getDate() + "\t" + t.getAmount() + "\t" + clean(t.getCategory()));
                out.newLine();
            }
            for (Map.Entry<String, Long> e : accountBook.getBudgets().entrySet()) {
                out.write("B\t" + clean(e.getKey()) + "\t" + e.getValue());
                out.newLine();
            }
            for (FixedExpense f : accountBook.getFixedExpenses()) {
                out.write("F\t" + clean(f.getName()) + "\t" + f.getAmount());
                out.newLine();
            }
            SavingsGoal g = accountBook.getGoal();
            if (g != null) {
                out.write("G\t" + clean(g.getName()) + "\t" + g.getTargetAmount() + "\t" + g.getSavedAmount());
                out.newLine();
            }
            out.flush();
            lastError = "";
            return true;
        } catch (IOException e) {
            lastError = "파일을 저장하지 못했습니다: " + e.getMessage();
            return false;
        } finally {
            close(out);
        }
    }

    /**
     * 파일을 읽어 거래내역, 예산, 정기 지출, 저축 목표를 {@link AccountBook}에 복원한다.
     * 파일이 없거나 손상되어 읽을 수 없으면 가계부를 빈 상태로 초기화하고 false를 반환하며,
     * 원인은 {@link #getLastError()}로 확인한다.
     *
     * @param filePath 불러올 파일 경로
     * @return 불러오기에 성공하면 true, 파일이 없거나 손상되었으면 false
     */
    public boolean loadData(String filePath) {
        File file = new File(filePath);
        if (!file.isFile()) {
            accountBook.clear();
            lastError = "저장된 파일이 없습니다.";
            return false;
        }
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            if (!HEADER.equals(in.readLine())) {
                throw new IllegalArgumentException("파일 머리글이 올바르지 않습니다.");
            }
            List<Transaction> transactions = new ArrayList<Transaction>();
            Map<String, Long> budgets = new LinkedHashMap<String, Long>();
            List<FixedExpense> fixed = new ArrayList<FixedExpense>();
            SavingsGoal goal = null;

            String line;
            while ((line = in.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] p = line.split("\t", -1);
                if ("T".equals(p[0]) && p.length == 5) {
                    if (!Transaction.INCOME.equals(p[1]) && !Transaction.EXPENSE.equals(p[1])) {
                        throw new IllegalArgumentException("구분 값 오류");
                    }
                    transactions.add(new Transaction(p[1], LocalDate.parse(p[2]), nonNegative(p[3]), p[4]));
                } else if ("B".equals(p[0]) && p.length == 3) {
                    budgets.put(p[1], nonNegative(p[2]));
                } else if ("F".equals(p[0]) && p.length == 3) {
                    fixed.add(new FixedExpense(p[1], nonNegative(p[2])));
                } else if ("G".equals(p[0]) && p.length == 4) {
                    goal = new SavingsGoal(p[1], nonNegative(p[2]), nonNegative(p[3]));
                } else {
                    throw new IllegalArgumentException("알 수 없는 줄: " + line);
                }
            }
            accountBook.restore(transactions, budgets, fixed, goal);
            lastError = "";
            return true;
        } catch (Exception e) {
            accountBook.clear();
            lastError = "파일이 손상되어 읽을 수 없습니다. 빈 상태로 시작합니다. (" + e.getMessage() + ")";
            return false;
        } finally {
            close(in);
        }
    }

    /**
     * 마지막 저장/불러오기가 실패한 이유를 돌려준다.
     *
     * @return 오류 설명. 마지막 작업이 성공했으면 빈 문자열
     */
    public String getLastError() {
        return lastError;
    }

    private static long nonNegative(String s) {
        long v = Long.parseLong(s);
        if (v < 0) {
            throw new IllegalArgumentException("음수 금액");
        }
        return v;
    }

    private static String clean(String s) {
        return s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }

    private static void close(java.io.Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
                // 닫기 실패는 무시한다.
            }
        }
    }
}
