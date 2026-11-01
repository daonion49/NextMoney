package nextmoney;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * 정기 지출 항목과 저축 목표를 관리하고, 다음 달 예상 고정지출 합계와 달성률을 보여 주는 화면(UC-03).
 */
public class GoalPanel extends JPanel {
    /** 직렬화 버전 번호. */
    private static final long serialVersionUID = 1L;

    private final AccountBook accountBook;
    private final MainFrame frame;

    private final JTextField fixedNameField = new JTextField(10);
    private final JTextField fixedAmountField = new JTextField(9);
    private final DefaultTableModel model = new DefaultTableModel(new String[] {"정기 지출 이름", "금액(원)"}, 0) {
        /** 직렬화 버전 번호. */
        private static final long serialVersionUID = 1L;
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable fixedTable = new JTable(model);
    private final JLabel totalLabel = new JLabel();
    private final JTextField goalNameField = new JTextField(10);
    private final JTextField goalTargetField = new JTextField(9);
    private final JTextField goalSavedField = new JTextField(9);
    private final JProgressBar goalBar = new JProgressBar(0, 100);
    private boolean refreshing = false;

    /**
     * 정기 지출·저축 목표 화면을 만든다.
     *
     * @param accountBook 데이터를 관리하는 가계부
     * @param frame       화면 갱신을 요청할 메인 프레임
     */
    public GoalPanel(AccountBook accountBook, MainFrame frame) {
        this.accountBook = accountBook;
        this.frame = frame;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel fixedPanel = new JPanel(new BorderLayout(6, 6));
        fixedPanel.setBorder(BorderFactory.createTitledBorder("정기 지출 관리"));
        JPanel fixedForm = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        fixedForm.add(new JLabel("이름"));
        fixedForm.add(fixedNameField);
        fixedForm.add(new JLabel("금액(원)"));
        fixedForm.add(fixedAmountField);
        JButton add = new JButton("추가");
        JButton update = new JButton("수정");
        JButton delete = new JButton("삭제");
        fixedForm.add(add);
        fixedForm.add(update);
        fixedForm.add(delete);
        fixedPanel.add(fixedForm, BorderLayout.NORTH);
        fixedTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fixedTable.setRowHeight(22);
        fixedPanel.add(new JScrollPane(fixedTable), BorderLayout.CENTER);
        fixedPanel.add(totalLabel, BorderLayout.SOUTH);

        JPanel goalPanel = new JPanel(new BorderLayout(6, 6));
        goalPanel.setBorder(BorderFactory.createTitledBorder("저축 목표"));
        JPanel goalForm = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        goalForm.add(new JLabel("목표 이름"));
        goalForm.add(goalNameField);
        goalForm.add(new JLabel("목표 금액"));
        goalForm.add(goalTargetField);
        goalForm.add(new JLabel("현재 저축액"));
        goalForm.add(goalSavedField);
        JButton saveGoal = new JButton("목표 저장");
        goalForm.add(saveGoal);
        goalPanel.add(goalForm, BorderLayout.NORTH);
        goalBar.setStringPainted(true);
        goalPanel.add(goalBar, BorderLayout.CENTER);

        add(fixedPanel);
        add(goalPanel);

        add.addActionListener(e -> onAddFixed());
        update.addActionListener(e -> onUpdateFixed());
        delete.addActionListener(e -> onDeleteFixed());
        saveGoal.addActionListener(e -> onSaveGoal());
        fixedTable.getSelectionModel().addListSelectionListener(e -> {
            int row = fixedTable.getSelectedRow();
            if (!e.getValueIsAdjusting() && !refreshing && row >= 0) {
                FixedExpense f = accountBook.getFixedExpenses().get(row);
                fixedNameField.setText(f.getName());
                fixedAmountField.setText(String.valueOf(f.getAmount()));
            }
        });
        refreshSummary();
    }

    /** [추가] 버튼: 정기 지출 항목을 등록하고 예상 고정지출 합계를 갱신한다. */
    public void onAddFixed() {
        if (fixedNameField.getText().trim().isEmpty() || fixedAmountField.getText().trim().isEmpty()) {
            showError("정기 지출의 이름과 금액을 모두 입력하세요.");
            return;
        }
        FixedGoalSummary s = accountBook.saveFixedAndGoal(fixedNameField.getText(), fixedAmountField.getText(), "", "", "");
        if (s == null) {
            showError("금액은 0 이상의 숫자로 입력하세요.");
            return;
        }
        fixedNameField.setText("");
        fixedAmountField.setText("");
        frame.refreshAll();
    }

    /** [수정] 버튼: 선택한 정기 지출 항목을 입력칸의 값으로 수정한다. */
    public void onUpdateFixed() {
        int row = fixedTable.getSelectedRow();
        if (row < 0) {
            showError("항목을 선택하세요.");
            return;
        }
        if (!accountBook.updateFixedExpense(row, fixedNameField.getText(), fixedAmountField.getText())) {
            showError("이름을 입력하고 금액은 0 이상의 숫자로 입력하세요.");
            return;
        }
        frame.refreshAll();
    }

    /** [삭제] 버튼: 선택한 정기 지출 항목을 삭제한다. */
    public void onDeleteFixed() {
        int row = fixedTable.getSelectedRow();
        if (row < 0) {
            showError("항목을 선택하세요.");
            return;
        }
        if (accountBook.deleteFixedExpense(row)) {
            fixedNameField.setText("");
            fixedAmountField.setText("");
            frame.refreshAll();
        }
    }

    /** [목표 저장] 버튼: 저축 목표를 저장하고 달성률을 진행 바로 표시한다. */
    public void onSaveGoal() {
        if (goalNameField.getText().trim().isEmpty() || goalTargetField.getText().trim().isEmpty()) {
            showError("목표 이름과 목표 금액을 입력하세요.");
            return;
        }
        FixedGoalSummary s = accountBook.saveFixedAndGoal("", "", goalNameField.getText(),
                goalTargetField.getText(), goalSavedField.getText());
        if (s == null) {
            showError("목표 금액은 0보다 큰 숫자, 현재 저축액은 0 이상의 숫자로 입력하세요.");
            return;
        }
        frame.refreshAll();
    }

    /** 정기 지출 목록, 예상 고정지출 합계, 저축 목표 입력칸과 달성률 진행 바를 다시 표시한다. */
    public void refreshSummary() {
        refreshing = true;
        model.setRowCount(0);
        List<FixedExpense> list = accountBook.getFixedExpenses();
        for (FixedExpense f : list) {
            model.addRow(new Object[] {f.getName(), String.format("%,d", f.getAmount())});
        }
        refreshing = false;
        FixedGoalSummary s = accountBook.summary();
        totalLabel.setText("다음 달 예상 고정지출(합계): " + AccountBook.won(s.getFixedTotal()));
        SavingsGoal g = accountBook.getGoal();
        if (g != null) {
            goalNameField.setText(g.getName());
            goalTargetField.setText(String.valueOf(g.getTargetAmount()));
            goalSavedField.setText(String.valueOf(g.getSavedAmount()));
            goalBar.setValue(s.getProgress());
            goalBar.setString("달성률 " + s.getProgress() + "%  (" + AccountBook.won(g.getSavedAmount())
                    + " / " + AccountBook.won(g.getTargetAmount()) + ")");
        } else {
            goalBar.setValue(0);
            goalBar.setString("저축 목표를 설정하세요");
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "입력 오류", JOptionPane.ERROR_MESSAGE);
    }
}
