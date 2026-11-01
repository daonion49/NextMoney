package nextmoney;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * 수입/지출 거래내역을 등록, 조회, 수정, 삭제하는 화면(UC-01).
 *
 * <p>입력값 검증은 {@link AccountBook}이 하고, 이 클래스는 입력을 전달해 결과를 표시한다.</p>
 */
public class TransactionPanel extends JPanel {
    /** 직렬화 버전 번호. */
    private static final long serialVersionUID = 1L;

    private final AccountBook accountBook;
    private final MainFrame frame;

    private final JComboBox<String> typeBox = new JComboBox<String>(new String[] {Transaction.EXPENSE, Transaction.INCOME});
    private final JTextField dateField = new JTextField(LocalDate.now().toString(), 9);
    private final JTextField amountField = new JTextField(9);
    private final JComboBox<String> categoryBox = new JComboBox<String>(AccountBook.CATEGORIES);
    private final DefaultTableModel model = new DefaultTableModel(new String[] {"날짜", "구분", "카테고리", "금액(원)"}, 0) {
        /** 직렬화 버전 번호. */
        private static final long serialVersionUID = 1L;
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private boolean refreshing = false;

    /**
     * 거래내역 화면을 만든다.
     *
     * @param accountBook 데이터를 관리하는 가계부
     * @param frame       화면 갱신과 경고 표시를 요청할 메인 프레임
     */
    public TransactionPanel(AccountBook accountBook, MainFrame frame) {
        this.accountBook = accountBook;
        this.frame = frame;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        form.setBorder(BorderFactory.createTitledBorder("거래내역 입력"));
        form.add(new JLabel("구분"));
        form.add(typeBox);
        form.add(new JLabel("날짜"));
        form.add(dateField);
        form.add(new JLabel("금액"));
        form.add(amountField);
        form.add(new JLabel("카테고리"));
        form.add(categoryBox);
        JButton register = new JButton("등록");
        JButton update = new JButton("수정");
        JButton delete = new JButton("삭제");
        form.add(register);
        form.add(update);
        form.add(delete);
        add(form, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(22);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(new JLabel("날짜는 yyyy-MM-dd 형식, 금액은 숫자로 입력합니다. 목록에서 행을 선택하면 입력칸에 채워집니다."), BorderLayout.SOUTH);

        register.addActionListener(e -> onRegister());
        update.addActionListener(e -> onUpdate());
        delete.addActionListener(e -> onDelete());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !refreshing && table.getSelectedRow() >= 0) {
                fillForm(table.getSelectedRow());
            }
        });
        refreshTable();
    }

    /** [등록] 버튼: 입력값을 {@link AccountBook}에 전달해 거래내역을 추가한다. */
    public void onRegister() {
        String category = (String) categoryBox.getSelectedItem();
        Transaction t = accountBook.registerTransaction((String) typeBox.getSelectedItem(),
                dateField.getText(), amountField.getText(), category);
        if (t == null) {
            showInputError();
            return;
        }
        amountField.setText("");
        afterChange(t);
    }

    /** [수정] 버튼: 목록에서 선택한 거래내역을 입력칸의 값으로 수정한다. */
    public void onUpdate() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "항목을 선택하세요.", "알림", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Transaction t = accountBook.updateTransaction(row, (String) typeBox.getSelectedItem(),
                dateField.getText(), amountField.getText(), (String) categoryBox.getSelectedItem());
        if (t == null) {
            showInputError();
            return;
        }
        afterChange(t);
        if (row < table.getRowCount()) {
            table.setRowSelectionInterval(row, row);
        }
    }

    /** [삭제] 버튼: 선택한 거래내역을 확인 후 삭제한다. */
    public void onDelete() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "항목을 선택하세요.", "알림", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this, "선택한 거래내역을 삭제할까요?", "삭제 확인",
                JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION && accountBook.deleteTransaction(row)) {
            frame.refreshAll();
        }
    }

    /** 가계부의 거래내역으로 JTable 목록을 다시 채운다. */
    public void refreshTable() {
        refreshing = true;
        model.setRowCount(0);
        List<Transaction> list = accountBook.getTransactions();
        for (Transaction t : list) {
            model.addRow(new Object[] {t.getDate().toString(), t.getType(),
                t.getCategory(), String.format("%,d", t.getAmount())});
        }
        refreshing = false;
    }

    private void afterChange(Transaction t) {
        BudgetStatus status = t.isExpense() ? accountBook.applyBudget(t.getCategory(), null) : null;
        frame.refreshAll();
        if (status != null && status.getWarningLevel() > 0) {
            frame.showBudgetWarning(status);
        }
    }

    private void fillForm(int row) {
        Transaction t = accountBook.getTransactions().get(row);
        typeBox.setSelectedItem(t.getType());
        dateField.setText(t.getDate().toString());
        amountField.setText(String.valueOf(t.getAmount()));
        categoryBox.setSelectedItem(t.getCategory());
    }

    private void showInputError() {
        JOptionPane.showMessageDialog(this,
                "입력값을 확인하세요.\n- 모든 항목을 입력해야 합니다.\n- 날짜는 yyyy-MM-dd 형식이어야 합니다.\n- 금액은 0보다 큰 숫자여야 합니다.",
                "입력 오류", JOptionPane.ERROR_MESSAGE);
    }
}
