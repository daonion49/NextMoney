package nextmoney;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

/**
 * 넥스트 머니(NextMoney)의 메인 창이자 프로그램 진입점.
 *
 * <p>하나의 JFrame에 4개의 탭(거래내역, 예산, 정기 지출·저축 목표, 예상 잔액·통계)을 두고,
 * 모든 탭 아래에 공통 [저장]/[불러오기] 바를 제공한다. 프로그램을 시작할 때 저장 파일을 읽고
 * 종료할 때 자동으로 저장한다.</p>
 */
public class MainFrame extends JFrame {
    /** 직렬화 버전 번호. */
    private static final long serialVersionUID = 1L;

    /** 데이터를 저장하는 파일 이름. 실행 위치와 상관없이 쓸 수 있는 사용자 폴더에 만들어진다. */
    public static final String DATA_FILE = "nextmoney_data.txt";

    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final AccountBook accountBook = new AccountBook();
    private final DataStore dataStore = new DataStore(accountBook);
    private final TransactionPanel transactionPanel;
    private final BudgetPanel budgetPanel;
    private final GoalPanel goalPanel;
    private final StatsPanel statsPanel;
    private final JLabel statusLabel = new JLabel(" ");

    /** 메인 창을 만들고 저장 파일이 있으면 불러온다. */
    public MainFrame() {
        super("넥스트 머니 (NextMoney) - 가계부");
        transactionPanel = new TransactionPanel(accountBook, this);
        budgetPanel = new BudgetPanel(accountBook);
        goalPanel = new GoalPanel(accountBook, this);
        statsPanel = new StatsPanel(accountBook);

        tabbedPane.addTab("거래내역", transactionPanel);
        tabbedPane.addTab("예산", budgetPanel);
        tabbedPane.addTab("정기 지출·저축 목표", goalPanel);
        tabbedPane.addTab("예상 잔액·통계", statsPanel);
        tabbedPane.addChangeListener(e -> refreshAll());

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, java.awt.Color.LIGHT_GRAY));
        JButton save = new JButton("저장");
        JButton load = new JButton("불러오기");
        bar.add(save);
        bar.add(load);
        bar.add(statusLabel);

        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);
        add(bar, BorderLayout.SOUTH);
        save.addActionListener(e -> onSave());
        load.addActionListener(e -> onLoad());

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exit();
            }
        });
        setSize(900, 620);
        setLocationRelativeTo(null);

        loadAtStartup();
    }

    /** 모든 탭의 화면을 가계부의 최신 데이터로 다시 그린다. */
    public void refreshAll() {
        transactionPanel.refreshTable();
        budgetPanel.refreshStatus();
        goalPanel.refreshSummary();
    }

    /**
     * 지출 경고 메시지를 대화상자로 보여 준다.
     *
     * @param status 경고 단계가 1 이상인 예산 현황
     */
    public void showBudgetWarning(BudgetStatus status) {
        JOptionPane.showMessageDialog(this, status.getWarningMessage(), "지출 경고", JOptionPane.WARNING_MESSAGE);
    }

    /** [저장] 버튼: 모든 데이터를 파일에 저장하고 결과를 알린다. */
    public void onSave() {
        if (dataStore.saveData(dataFilePath())) {
            statusLabel.setText("저장 완료: " + dataFilePath());
            JOptionPane.showMessageDialog(this, "데이터를 저장했습니다.", "저장", JOptionPane.INFORMATION_MESSAGE);
        } else {
            statusLabel.setText("저장 실패");
            JOptionPane.showMessageDialog(this, dataStore.getLastError() + "\n잠시 후 다시 시도하세요.", "저장 오류",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /** [불러오기] 버튼: 저장된 파일에서 데이터를 복원하고 화면에 반영한다. */
    public void onLoad() {
        boolean ok = dataStore.loadData(dataFilePath());
        refreshAll();
        statsPanel.reset();
        if (ok) {
            statusLabel.setText("불러오기 완료");
        } else {
            statusLabel.setText("불러오기 실패 - 빈 상태");
            JOptionPane.showMessageDialog(this, dataStore.getLastError(), "불러오기 오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadAtStartup() {
        if (!new File(dataFilePath()).isFile()) {
            statusLabel.setText("저장된 파일이 없어 빈 상태로 시작합니다.");
            return;
        }
        boolean ok = dataStore.loadData(dataFilePath());
        refreshAll();
        if (ok) {
            statusLabel.setText("저장된 데이터를 불러왔습니다.");
        } else {
            statusLabel.setText("파일 오류 - 빈 상태로 시작");
            JOptionPane.showMessageDialog(this, dataStore.getLastError(), "불러오기 오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exit() {
        if (!dataStore.saveData(dataFilePath())) {
            int a = JOptionPane.showConfirmDialog(this, dataStore.getLastError() + "\n저장하지 않고 종료할까요?",
                    "저장 오류", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
            if (a != JOptionPane.YES_OPTION) {
                return;
            }
        }
        dispose();
        System.exit(0);
    }

    /**
     * 데이터 파일의 전체 경로를 정한다. 쓰기 권한 문제를 피하려고 실행한 폴더가 아니라
     * 사용자 홈 폴더를 먼저 쓰고, 그곳에 쓸 수 없으면 임시 폴더, 그것도 안 되면 현재 폴더를 쓴다.
     *
     * @return 데이터 파일 경로
     */
    private static String dataFilePath() {
        String[] candidates = {System.getProperty("user.home"), System.getProperty("java.io.tmpdir")};
        for (String dir : candidates) {
            if (dir != null && new File(dir).isDirectory() && new File(dir).canWrite()) {
                return new File(dir, DATA_FILE).getPath();
            }
        }
        return DATA_FILE;
    }

    private static void applyFont() {
        Set<String> available = new HashSet<String>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        String family = "Dialog";
        for (String f : new String[] {"맑은 고딕", "Malgun Gothic", "Apple SD Gothic Neo", "NanumGothic", "Noto Sans CJK KR"}) {
            if (available.contains(f)) {
                family = f;
                break;
            }
        }
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (UIManager.get(key) instanceof FontUIResource) {
                UIManager.put(key, new FontUIResource(family, Font.PLAIN, 13));
            }
        }
    }

    /**
     * 프로그램을 시작한다.
     *
     * @param args 사용하지 않는다
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // 기본 Look and Feel을 사용한다.
            }
            applyFont();
            new MainFrame().setVisible(true);
        });
    }
}
