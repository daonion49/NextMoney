package nextmoney;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.YearMonth;
import java.util.Collections;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * 다음 달 예상 잔액을 계산해 보여 주고, 월·카테고리별 지출 통계를 막대그래프로 표시하는 화면(UC-04).
 */
public class StatsPanel extends JPanel {
    /** 직렬화 버전 번호. */
    private static final long serialVersionUID = 1L;

    private final AccountBook accountBook;

    private final JTextField incomeField = new JTextField(10);
    private final JLabel resultLabel = new JLabel("예상 수입을 입력하고 [예상 잔액 계산]을 누르세요.");
    private final JComboBox<String> monthBox = new JComboBox<String>();
    private final JComboBox<String> categoryBox = new JComboBox<String>();
    private final BarChartPanel chartPanel = new BarChartPanel();

    /**
     * 예상 잔액·통계 화면을 만든다.
     *
     * @param accountBook 데이터를 관리하는 가계부
     */
    public StatsPanel(AccountBook accountBook) {
        this.accountBook = accountBook;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel forecast = new JPanel(new BorderLayout(4, 4));
        forecast.setBorder(BorderFactory.createTitledBorder("다음 달(" + YearMonth.now().plusMonths(1) + ") 예상 잔액"));
        JPanel incomeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        incomeRow.add(new JLabel("다음 달 예상 수입(원)"));
        incomeRow.add(incomeField);
        JButton calc = new JButton("예상 잔액 계산");
        incomeRow.add(calc);
        forecast.add(incomeRow, BorderLayout.NORTH);
        forecast.add(resultLabel, BorderLayout.CENTER);
        add(forecast, BorderLayout.NORTH);

        for (int i = 0; i < 12; i++) {
            monthBox.addItem(YearMonth.now().minusMonths(i).toString());
        }
        categoryBox.addItem(AccountBook.ALL);
        for (String c : AccountBook.CATEGORIES) {
            categoryBox.addItem(c);
        }
        JPanel stats = new JPanel(new BorderLayout(4, 4));
        stats.setBorder(BorderFactory.createTitledBorder("월별·카테고리별 지출 통계"));
        JPanel queryRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        queryRow.add(new JLabel("조회 월"));
        queryRow.add(monthBox);
        queryRow.add(new JLabel("카테고리"));
        queryRow.add(categoryBox);
        JButton query = new JButton("조회");
        queryRow.add(query);
        stats.add(queryRow, BorderLayout.NORTH);
        stats.add(chartPanel, BorderLayout.CENTER);
        add(stats, BorderLayout.CENTER);

        calc.addActionListener(e -> onForecast());
        query.addActionListener(e -> onQuery());
    }

    /** [예상 잔액 계산] 버튼: 예상 수입을 전달해 다음 달 예상 잔액을 계산하고 결과를 표시한다. */
    public void onForecast() {
        ForecastResult r = accountBook.forecastBalance(incomeField.getText(),
                (String) monthBox.getSelectedItem(), (String) categoryBox.getSelectedItem());
        if (r == null) {
            JOptionPane.showMessageDialog(this, "예상 수입은 숫자로 입력하세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            return;
        }
        StringBuilder sb = new StringBuilder("<html><table cellspacing='2'>");
        sb.append(row("예상 수입", AccountBook.won(r.getIncome()), false));
        sb.append(row("예상 고정지출", "- " + AccountBook.won(r.getFixedTotal()), false));
        sb.append(row("최근 평균 변동지출", "- " + AccountBook.won(r.getAvgVariable()), false));
        sb.append(row("저축 목표액", "- " + AccountBook.won(r.getGoalAmount()), false));
        sb.append(row("다음 달 예상 잔액", AccountBook.won(r.getExpectedBalance()), true));
        sb.append("</table>");
        if (r.getExpectedBalance() < 0) {
            sb.append("<font color='#C62828'>⚠ ").append(AccountBook.won(r.getShortage()))
              .append(" 부족합니다. 지출을 줄이거나 저축 목표를 조정하세요.</font><br>");
        }
        if (r.isInsufficientData()) {
            sb.append("<font color='#555555'>최근 지출 데이터가 없어 평균 변동지출을 0원으로 계산했습니다.</font>");
        }
        sb.append("</html>");
        resultLabel.setText(sb.toString());
        drawBarChart(r.getCategorySums());
    }


    private static String row(String label, String value, boolean strong) {
        String l = strong ? "<b>" + label + "</b>" : label;
        String v = strong ? "<b><font color='#1565C0'>" + value + "</font></b>" : value;
        return "<tr><td width='170'>" + l + "</td><td align='right' width='140'>" + v + "</td></tr>";
    }

    /** [조회] 버튼: 선택한 월과 카테고리의 지출을 집계해 막대그래프로 표시한다. */
    public void onQuery() {
        String income = incomeField.getText().trim().isEmpty() ? "0" : incomeField.getText();
        ForecastResult r = accountBook.forecastBalance(income, (String) monthBox.getSelectedItem(),
                (String) categoryBox.getSelectedItem());
        if (r == null) {
            JOptionPane.showMessageDialog(this, "예상 수입은 숫자로 입력하세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            return;
        }
        drawBarChart(r.getCategorySums());
    }

    /**
     * 카테고리별 지출 합계를 막대그래프로 그린다. 데이터가 없으면 "조회 결과가 없습니다"를 표시한다.
     *
     * @param sums 카테고리 이름과 지출 합계
     */
    public void drawBarChart(Map<String, Long> sums) {
        chartPanel.setData(sums);
    }

    /** 통계 화면을 처음 상태로 되돌린다(데이터를 불러온 뒤 호출). */
    public void reset() {
        chartPanel.setData(Collections.<String, Long>emptyMap());
    }

    /** 카테고리별 지출 막대그래프를 그리는 내부 패널. */
    private static class BarChartPanel extends JPanel {
        /** 직렬화 버전 번호. */
        private static final long serialVersionUID = 1L;
        private static final Color[] COLORS = {
            new Color(66, 133, 244), new Color(52, 168, 83), new Color(251, 188, 5),
            new Color(234, 67, 53), new Color(171, 71, 188), new Color(0, 172, 193)
        };
        private Map<String, Long> data = Collections.emptyMap();
        private boolean queried = false;

        BarChartPanel() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(400, 220));
        }

        void setData(Map<String, Long> sums) {
            this.data = sums;
            this.queried = true;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            FontMetrics fm = g.getFontMetrics();
            if (data.isEmpty()) {
                g.setColor(Color.GRAY);
                String msg = queried ? "조회 결과가 없습니다" : "조회 버튼을 누르면 카테고리별 지출이 막대그래프로 표시됩니다";
                g.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
                return;
            }
            long max = 1;
            for (Long v : data.values()) {
                max = Math.max(max, v);
            }
            int n = data.size();
            int left = 30;
            int bottom = getHeight() - 28;
            int top = 28;
            int slot = Math.max(40, (getWidth() - left * 2) / n);
            int barWidth = Math.min(70, slot - 16);
            g.setColor(Color.LIGHT_GRAY);
            g.drawLine(left, bottom, getWidth() - left, bottom);
            int i = 0;
            for (Map.Entry<String, Long> e : data.entrySet()) {
                int x = left + i * slot + (slot - barWidth) / 2;
                int h = (int) ((bottom - top) * e.getValue() / max);
                g.setColor(COLORS[i % COLORS.length]);
                g.fillRect(x, bottom - h, barWidth, h);
                g.setColor(Color.DARK_GRAY);
                String value = String.format("%,d", e.getValue());
                g.drawString(value, x + (barWidth - fm.stringWidth(value)) / 2, bottom - h - 4);
                String name = e.getKey();
                g.drawString(name, x + (barWidth - fm.stringWidth(name)) / 2, bottom + 16);
                i++;
            }
        }
    }
}
