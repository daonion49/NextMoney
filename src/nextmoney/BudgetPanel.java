package nextmoney;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.time.YearMonth;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * 카테고리별 월 예산을 설정하고 예산 현황과 1~3차 지출 경고를 보여 주는 화면(UC-02).
 */
public class BudgetPanel extends JPanel {
    /** 직렬화 버전 번호. */
    private static final long serialVersionUID = 1L;

    private final AccountBook accountBook;

    private final JComboBox<String> categoryBox = new JComboBox<String>(AccountBook.CATEGORIES);
    private final JTextField budgetField = new JTextField(10);
    private final DefaultTableModel model = new DefaultTableModel(
            new String[] {"카테고리", "월 예산", "현재 지출", "남은 예산", "사용률"}, 0) {
        /** 직렬화 버전 번호. */
        private static final long serialVersionUID = 1L;
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable statusTable = new JTable(model);
    private final JProgressBar usageBar = new JProgressBar(0, 100);
    private final JLabel warningLabel = new JLabel(" ");

    /**
     * 예산 화면을 만든다.
     *
     * @param accountBook 데이터를 관리하는 가계부
     */
    public BudgetPanel(AccountBook accountBook) {
        this.accountBook = accountBook;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        form.setBorder(BorderFactory.createTitledBorder("월 예산 설정 (기준 월: " + YearMonth.now() + ")"));
        form.add(new JLabel("카테고리"));
        form.add(categoryBox);
        form.add(new JLabel("월 예산(원)"));
        form.add(budgetField);
        JButton save = new JButton("예산 저장");
        form.add(save);
        add(form, BorderLayout.NORTH);

        statusTable.setRowHeight(22);
        add(new JScrollPane(statusTable), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(4, 4));
        bottom.setBorder(BorderFactory.createTitledBorder("선택한 카테고리의 예산 사용률"));
        usageBar.setStringPainted(true);
        bottom.add(usageBar, BorderLayout.CENTER);
        bottom.add(warningLabel, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        save.addActionListener(e -> onSaveBudget());
        categoryBox.addActionListener(e -> refreshStatus());
        refreshStatus();
    }

    /** [예산 저장] 버튼: 선택한 카테고리의 월 예산을 저장하고 현황과 경고를 갱신한다. */
    public void onSaveBudget() {
        String category = (String) categoryBox.getSelectedItem();
        BudgetStatus status = accountBook.applyBudget(category, budgetField.getText());
        if (status == null) {
            JOptionPane.showMessageDialog(this, "월 예산은 0보다 큰 숫자로 입력하세요.", "입력 오류",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        budgetField.setText("");
        refreshStatus();
        if (status.getWarningLevel() > 0) {
            showWarning(status.getWarningLevel());
        }
    }

    /**
     * 경고 단계에 맞는 메시지를 대화상자로 보여 준다.
     *
     * @param level 경고 단계(1~3)
     */
    public void showWarning(int level) {
        BudgetStatus status = accountBook.applyBudget((String) categoryBox.getSelectedItem(), null);
        if (status != null && status.getWarningLevel() == level) {
            JOptionPane.showMessageDialog(this, status.getWarningMessage(), "지출 경고", JOptionPane.WARNING_MESSAGE);
        }
    }

    /** 예산 현황 표, 선택한 카테고리의 사용률 바, 경고 문구를 다시 계산해 표시한다. */
    public void refreshStatus() {
        model.setRowCount(0);
        for (String category : accountBook.getBudgets().keySet()) {
            BudgetStatus s = accountBook.applyBudget(category, null);
            model.addRow(new Object[] {category, AccountBook.won(s.getBudget()), AccountBook.won(s.getSpent()),
                AccountBook.won(s.getRemaining()), String.format("%.0f%%", s.getUsageRate())});
        }
        BudgetStatus sel = accountBook.applyBudget((String) categoryBox.getSelectedItem(), null);
        if (sel == null || !sel.hasBudget()) {
            usageBar.setValue(0);
            usageBar.setString("예산 없음");
            warningLabel.setText("이 카테고리는 예산이 설정되지 않았습니다.");
            warningLabel.setForeground(Color.DARK_GRAY);
            return;
        }
        usageBar.setValue((int) Math.min(100, Math.round(sel.getUsageRate())));
        usageBar.setString(String.format("%.0f%%", sel.getUsageRate()));
        int level = sel.getWarningLevel();
        usageBar.setForeground(level == 0 ? new Color(76, 175, 80) : level == 1 ? new Color(255, 193, 7)
                : level == 2 ? new Color(255, 112, 67) : new Color(211, 47, 47));
        warningLabel.setText(level == 0 ? "예산 범위 안에서 사용 중입니다." : sel.getWarningMessage());
        warningLabel.setForeground(level == 0 ? new Color(46, 125, 50) : new Color(198, 40, 40));
    }
}
