package view;

import controller.Game;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import model.board.Position;
import model.pieces.Pawn;
import model.pieces.Piece;

public class ChessGUI extends JFrame {

    private static final long serialVersionUID = 1L;

    // --- Paleta de Cores e Tema ---
    private static final Color BG_COLOR = new Color(30, 32, 34);
    private static final Color COMPONENT_BG = new Color(45, 45, 45);
    private static final Color BORDER_COLOR = new Color(80, 80, 80);

// Casas do tabuleiro (roxo claro e roxo escuro)
    private static final Color BOARD_LIGHT_GREEN = new Color(186, 85, 211); // Roxo claro
    private static final Color BOARD_DARK_GREEN = new Color(75, 0, 130);   // Roxo escuro

// Botões no mesmo tom do roxo claro
    private static final Color BUTTON_GREEN = new Color(186, 85, 211);

    private static final Color ACCENT_WHITE = new Color(220, 220, 220);
    private static final Color ACCENT_CYAN = BOARD_DARK_GREEN;
    private static final Color ACCENT_YELLOW = new Color(240, 220, 90);
    private static final Color ACCENT_RED = new Color(220, 40, 40);

    private static final Color TIMER_ORANGE = new Color(252, 142, 42);
    private static final Color WHITE_PIECE_COLOR = Color.WHITE;
    private static final Color BLACK_PIECE_COLOR = TIMER_ORANGE;

// Bordas
    private static final Border BORDER_SELECTED = new LineBorder(ACCENT_RED, 3);
    private static final Border BORDER_LEGAL = new LineBorder(ACCENT_RED, 3);
    private static final Border BORDER_LASTMOVE = new LineBorder(ACCENT_YELLOW, 2, true);

    // Fontes
    private static final Font UI_FONT = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font HISTORY_FONT = new Font("Consolas", Font.PLAIN, 14);
    private static final Font TIMER_FONT = new Font("Segoe UI", Font.BOLD, 42);
    private static final Font OVERVIEW_TITLE_FONT = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font DIALOG_FONT = new Font("Segoe UI", Font.BOLD, 16);

    private final Game game;
    private final JPanel boardPanel;
    private final JPanel[][] squares = new JPanel[8][8];
    private final JLabel status;
    private JList<String> historyList;
    private DefaultListModel<String> historyModel;
    private JComboBox<Integer> difficultyComboBox;
    private JToggleButton aiToggle;
    private Position selected = null;
    private List<Position> legalForSelected = new ArrayList<>();
    private Position lastFrom = null, lastTo = null;
    private boolean aiThinking = false;
    private final Random rnd = new Random();

    private boolean isPcPlayingBlack = false;

    private JLabel whiteTimerLabel, blackTimerLabel;
    private Timer gameTimer;
    private long whiteTimeMillis, blackTimeMillis;
    private long initialTimeMillis;
    private long incrementMillis;
    private JComboBox<String> timeControlComboBox;
    private boolean isGameActive = false;

    public ChessGUI() {
        super("ChessGame");
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ignored) {
        }

        this.game = new Game();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        boardPanel = new JPanel(new GridLayout(8, 8, 0, 0));
        boardPanel.setBackground(BG_COLOR);
        boardPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                final int rr = r;
                final int cc = c;
                JPanel p = new JPanel(new BorderLayout());
                p.setOpaque(true);

                p.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        handleClick(new Position(rr, cc));
                    }
                });
                squares[r][c] = p;
                boardPanel.add(p);
            }
        }

        status = new JLabel("Clique em 'Iniciar Jogo' para preparar a partida.");
        status.setFont(UI_FONT);
        status.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        status.setForeground(BOARD_LIGHT_GREEN);
        status.setBackground(new Color(25, 27, 29));
        status.setOpaque(true);

        add(boardPanel, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        add(buildControlPanel(), BorderLayout.EAST);

        getContentPane().setBackground(BG_COLOR);

        boardPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                refresh();
            }
        });

        setMinimumSize(new Dimension(920, 680));
        setLocationRelativeTo(null);

        doNewGame();

        setVisible(true);
        refresh();
    }

    private JPanel buildControlPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBackground(BG_COLOR);
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 0);

        // --- Timers ---
        gbc.gridy = 0;
        blackTimerLabel = new JLabel("03:00");
        styleTimerLabel(blackTimerLabel, TIMER_ORANGE, COMPONENT_BG);
        mainPanel.add(blackTimerLabel, gbc);

        gbc.gridy = 1;
        gbc.insets.bottom = 15;
        whiteTimerLabel = new JLabel("03:00");
        styleTimerLabel(whiteTimerLabel, ACCENT_WHITE, ACCENT_CYAN);
        mainPanel.add(whiteTimerLabel, gbc);

        // --- Visão Geral da Partida (Histórico) ---
        gbc.gridy = 2;
        gbc.insets.bottom = 5;
        JLabel overviewLabel = new JLabel("Visão Geral da Partida");
        overviewLabel.setFont(OVERVIEW_TITLE_FONT);
        overviewLabel.setForeground(BOARD_LIGHT_GREEN);
        overviewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(overviewLabel, gbc);

        gbc.gridy = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        historyModel = new DefaultListModel<>();
        historyList = new JList<>(historyModel);
        styleHistoryList(historyList);
        JScrollPane historyScroll = new JScrollPane(historyList);
        historyScroll.setBorder(null);
        mainPanel.add(historyScroll, gbc);

        // --- Botões de Controle ---
        gbc.gridy = 4;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets.top = 15;
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        buttonPanel.setOpaque(false);
        JButton btnStart = createStyledButton("Iniciar Jogo", BUTTON_GREEN, BG_COLOR);

        btnStart.addActionListener(e -> {
            showModernEndgameDialog("Partida Iniciada!", "Mova a peça branca");
            doNewGame();
        });

        JButton btnRestart = createStyledButton("Reiniciar Jogo", BUTTON_GREEN, BG_COLOR);
        btnRestart.addActionListener(e -> doNewGame());
        buttonPanel.add(btnStart);
        buttonPanel.add(btnRestart);
        mainPanel.add(buttonPanel, gbc);

        gbc.insets.top = 10;
        gbc.gridy = 5;
        JPanel aiOpponentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        aiOpponentPanel.setOpaque(false);
        JLabel aiLabel = new JLabel("Oponente IA:");
        aiLabel.setFont(UI_FONT);
        aiLabel.setForeground(ACCENT_WHITE);
        aiOpponentPanel.add(aiLabel);
        aiOpponentPanel.add(Box.createHorizontalStrut(10));
        aiOpponentPanel.add(createAIToggle());
        mainPanel.add(aiOpponentPanel, gbc);

        gbc.gridy = 6;
        JPanel aiLevelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        aiLevelPanel.setOpaque(false);
        JLabel levelLabel = new JLabel("Nível IA:");
        levelLabel.setFont(UI_FONT);
        levelLabel.setForeground(ACCENT_WHITE);
        aiLevelPanel.add(levelLabel);
        aiLevelPanel.add(Box.createHorizontalStrut(10));
        Integer[] difficultyLevels = {1, 2};
        difficultyComboBox = new JComboBox<>(difficultyLevels);
        styleComboBox(difficultyComboBox);
        difficultyComboBox.setEnabled(isPcPlayingBlack);
        aiLevelPanel.add(difficultyComboBox);
        mainPanel.add(aiLevelPanel, gbc);

        gbc.gridy = 7;
        JPanel rhythmPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        rhythmPanel.setOpaque(false);
        JLabel rhythmLabel = new JLabel("Ritmo:");
        rhythmLabel.setFont(UI_FONT);
        rhythmLabel.setForeground(ACCENT_WHITE);
        rhythmPanel.add(rhythmLabel);
        rhythmPanel.add(Box.createHorizontalStrut(10));
        String[] timeControls = {"3 min", "2 min + 5s"};
        timeControlComboBox = new JComboBox<>(timeControls);
        timeControlComboBox.setSelectedIndex(0);
        styleComboBox(timeControlComboBox);
        timeControlComboBox.addActionListener(e -> doNewGame());
        rhythmPanel.add(timeControlComboBox);
        mainPanel.add(rhythmPanel, gbc);

        return mainPanel;
    }

    private JToggleButton createAIToggle() {
        aiToggle = new JToggleButton("OFF");
        aiToggle.setSelected(isPcPlayingBlack);
        aiToggle.setFont(UI_FONT);
        aiToggle.setFocusPainted(false);
        aiToggle.setBorder(null);
        updateAIToggleStyle();

        aiToggle.addActionListener(e -> {
            isPcPlayingBlack = aiToggle.isSelected();
            difficultyComboBox.setEnabled(isPcPlayingBlack);
            updateAIToggleStyle();
            doNewGame();
        });
        return aiToggle;
    }

    private void updateAIToggleStyle() {
        if (aiToggle.isSelected()) {
            aiToggle.setText("ON");
            aiToggle.setBackground(BUTTON_GREEN);
            aiToggle.setForeground(BG_COLOR);
            aiToggle.setBorder(null);
        } else {
            aiToggle.setText("OFF");
            aiToggle.setBackground(COMPONENT_BG);
            aiToggle.setForeground(ACCENT_WHITE);
            aiToggle.setBorder(null);
        }
    }

    private void styleTimerLabel(JLabel label, Color fgColor, Color bgColor) {
        label.setFont(TIMER_FONT);
        label.setForeground(fgColor);
        label.setBackground(bgColor);
        label.setOpaque(true);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
    }

    private void styleHistoryList(JList<String> list) {
        list.setBackground(COMPONENT_BG);
        list.setForeground(ACCENT_WHITE);
        list.setFont(HISTORY_FONT);
        list.setSelectionBackground(ACCENT_CYAN);
        list.setSelectionForeground(BG_COLOR);
        list.setFixedCellHeight(16);
        list.setBorder(new EmptyBorder(6, 6, 6, 6));
    }

    private JButton createStyledButton(String text, Color bgColor, Color fgColor) {
        JButton button = new JButton(text);
        button.setFont(UI_FONT);
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bgColor.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
        return button;
    }

    private void styleComboBox(JComboBox<?> cb) {
        cb.setFont(UI_FONT);
        cb.setBackground(COMPONENT_BG);
        cb.setForeground(ACCENT_WHITE);
        cb.setBorder(null);
    }

    private void doNewGame() {
        if (gameTimer != null) {
            gameTimer.stop();
        }

        selected = null;
        legalForSelected.clear();
        lastFrom = lastTo = null;
        aiThinking = false;
        isGameActive = false;
        game.newGame();
        historyModel.clear();

        String selectedTimeControl = (String) timeControlComboBox.getSelectedItem();
        if (selectedTimeControl != null && selectedTimeControl.contains("2 min + 5s")) {
            initialTimeMillis = 2 * 60 * 1000;
            incrementMillis = 5 * 1000;
        } else {
            initialTimeMillis = 3 * 60 * 1000;
            incrementMillis = 0;
        }

        whiteTimeMillis = initialTimeMillis;
        blackTimeMillis = initialTimeMillis;

        updateTimerLabels();
        refresh();
    }

    private void setupAndStartTimer() {
        if (gameTimer != null && gameTimer.isRunning()) {
            return;
        }
        gameTimer = new Timer(100, e -> {
            if (!isGameActive) {
                gameTimer.stop();
                return;
            }

            if (game.whiteToMove()) {
                whiteTimeMillis -= 100;
            } else {
                blackTimeMillis -= 100;
            }

            if (whiteTimeMillis <= 0) {
                whiteTimeMillis = 0;
                endGameByTime(true);
            }
            if (blackTimeMillis <= 0) {
                blackTimeMillis = 0;
                endGameByTime(false);
            }
            updateTimerLabels();
        });
        gameTimer.start();
    }

    private void updateTimerLabels() {
        whiteTimerLabel.setText(formatTime(whiteTimeMillis));
        blackTimerLabel.setText(formatTime(blackTimeMillis));
    }

    private String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void endGameByTime(boolean whiteLost) {
        isGameActive = false;
        if (gameTimer != null) {
            gameTimer.stop();
        }
        String winner = whiteLost ? "Laranjas" : "Brancas";
        String msg = "O tempo acabou! " + winner + " venceram.";

        showModernEndgameDialog("Fim de Jogo", msg);
        refresh();
    }

    private void handleClick(Position clicked) {
        if (game.isGameOver() || aiThinking) {
            return;
        }

        if (!isGameActive) {
            Piece p = game.board().get(clicked);
            if (p != null && p.isWhite() == game.whiteToMove()) {
                isGameActive = true;
                setupAndStartTimer();
            } else {
                return;
            }
        }

        if (isPcPlayingBlack && !game.whiteToMove()) {
            return;
        }

        Piece p = game.board().get(clicked);

        if (selected == null) {
            if (p != null && p.isWhite() == game.whiteToMove()) {
                selected = clicked;
                legalForSelected = game.legalMovesFrom(selected);
            }
        } else {
            if (game.legalMovesFrom(selected).contains(clicked)) {
                boolean isWhiteMoving = game.whiteToMove();

                Character promo = null;
                if (game.board().get(selected) instanceof Pawn && game.isPromotion(selected, clicked)) {
                    promo = askPromotion();
                }
                lastFrom = selected;
                lastTo = clicked;
                game.move(selected, clicked, promo);

                if (isWhiteMoving) {
                    whiteTimeMillis += incrementMillis;
                } else {
                    blackTimeMillis += incrementMillis;
                }
                updateTimerLabels();

                selected = null;
                legalForSelected.clear();
                refresh();
                maybeAnnounceEnd();
                maybeTriggerAI();
                return;
            } else if (p != null && p.isWhite() == game.whiteToMove()) {
                selected = clicked;
                legalForSelected = game.legalMovesFrom(selected);
            } else {
                selected = null;
                legalForSelected.clear();
            }
        }
        refresh();
    }

    private Character askPromotion() {
        String[] opts = {"Rainha", "Torre", "Bispo", "Cavalo"};
        UIManager.put("OptionPane.background", BG_COLOR);
        UIManager.put("Panel.background", BG_COLOR);
        UIManager.put("OptionPane.messageForeground", BOARD_LIGHT_GREEN);

        int ch = JOptionPane.showOptionDialog(this, "Escolha a peça para promoção:", "Promoção",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opts, opts[0]);

        UIManager.put("OptionPane.background", null);
        UIManager.put("Panel.background", null);
        UIManager.put("OptionPane.messageForeground", null);

        return switch (ch) {
            case 1 ->
                'R';
            case 2 ->
                'B';
            case 3 ->
                'N';
            default ->
                'Q';
        };
    }

    private void maybeTriggerAI() {
        if (game.isGameOver() || !isPcPlayingBlack || game.whiteToMove() || !isGameActive) {
            return;
        }

        aiThinking = true;
        refresh();

        new SwingWorker<Move, Void>() {
            @Override
            protected Move doInBackground() {
                int level = (Integer) difficultyComboBox.getSelectedItem();

                if (level <= 1) {
                    var allMoves = collectAllLegalMovesForSide(false);
                    if (allMoves.isEmpty()) {
                        return null;
                    }
                    return allMoves.get(rnd.nextInt(allMoves.size()));
                }

                if (level == 2) {
                    var allMoves = collectAllLegalMovesForSide(false);
                    if (allMoves.isEmpty()) {
                        return null;
                    }
                    return pickBestMoveLevel2(allMoves);
                }

                var allMoves = collectAllLegalMovesForSide(false);
                if (allMoves.isEmpty()) {
                    return null;
                }
                return allMoves.get(rnd.nextInt(allMoves.size()));
            }

            @Override
            protected void done() {
                try {
                    Move bestMove = get();
                    if (bestMove != null && !game.isGameOver() && isGameActive) {
                        lastFrom = bestMove.from();
                        lastTo = bestMove.to();
                        Character promo = (game.board().get(lastFrom) instanceof Pawn && game.isPromotion(lastFrom, lastTo)) ? 'Q' : null;

                        game.move(lastFrom, lastTo, promo);
                        blackTimeMillis += incrementMillis;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    aiThinking = false;
                    refresh();
                    maybeAnnounceEnd();
                }
            }
        }.execute();
    }

    private record Move(Position from, Position to) {

    }

    private List<Move> collectAllLegalMovesForSide(boolean whiteSide) {
        List<Move> moves = new ArrayList<>();
        if (whiteSide != game.whiteToMove()) {
            return moves;
        }
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Position from = new Position(r, c);
                if (game.board().get(from) != null && game.board().get(from).isWhite() == whiteSide) {
                    for (Position to : game.legalMovesFrom(from)) {
                        moves.add(new Move(from, to));
                    }
                }
            }
        }
        return moves;
    }

    private Move pickBestMoveLevel2(List<Move> allMoves) {
        class ScoredMove {

            Move m;
            double score;

            ScoredMove(Move m, double s) {
                this.m = m;
                this.score = s;
            }
        }
        List<ScoredMove> scored = new ArrayList<>();

        for (Move mv : allMoves) {
            Piece moving = game.board().get(mv.from());
            Piece captured = game.board().get(mv.to());
            double s = 0.0;

            if (captured != null) {
                s += pieceValue(captured) * 2.0;
            }

            if (game.board().get(mv.from()) instanceof Pawn && game.isPromotion(mv.from(), mv.to())) {
                s += 9000;
            }

            s += centerBonus(mv.to()) * 10;

            int mobility = game.legalMovesFrom(mv.from()).size();
            s += mobility * 3;

            s += rnd.nextDouble() * 5.0;

            scored.add(new ScoredMove(mv, s));
        }

        scored.sort(Comparator.comparingDouble((ScoredMove sm) -> sm.score).reversed());

        int K = Math.max(1, Math.min(8, scored.size()));
        double bestScore = scored.get(0).score;

        List<ScoredMove> topCandidates = new ArrayList<>();
        for (int i = 0; i < scored.size() && i < K; i++) {
            if (scored.get(i).score >= bestScore - 50) {
                topCandidates.add(scored.get(i));
            }
        }
        ScoredMove choice = topCandidates.get(rnd.nextInt(topCandidates.size()));
        return choice.m;
    }

    private boolean isSquareAttackedBy(Position targetSquare, boolean isWhiteAttacker, Game game) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Position currentPos = new Position(r, c);
                Piece piece = game.board().get(currentPos);
                if (piece != null && piece.isWhite() == isWhiteAttacker) {
                    if ("P".equalsIgnoreCase(piece.getSymbol())) {
                        int direction = isWhiteAttacker ? -1 : 1;
                        if (targetSquare.getRow() == r + direction && (targetSquare.getColumn() == c + 1 || targetSquare.getColumn() == c - 1)) {
                            return true;
                        }
                    } else {
                        if (piece.getPossibleMoves().contains(targetSquare)) {

                        }
                    }
                }
            }
        }
        return false;
    }

    private int pieceValue(Piece p) {
        if (p == null) {
            return 0;
        }
        return switch (p.getSymbol().toUpperCase()) {
            case "P" ->
                100;
            case "N", "B" ->
                300;
            case "R" ->
                500;
            case "Q" ->
                900;
            case "K" ->
                10000;
            default ->
                0;
        };
    }

    private int centerBonus(Position pos) {
        int r = pos.getRow(), c = pos.getColumn();
        if ((r == 3 || r == 4) && (c == 3 || c == 4)) {
            return 15;
        }
        if ((r >= 2 && r <= 5) && (c >= 2 && c <= 5)) {
            return 5;
        }
        return 0;
    }

    private void refresh() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                JPanel squarePanel = squares[r][c];
                squarePanel.setBackground((r + c) % 2 == 0 ? BOARD_LIGHT_GREEN : BOARD_DARK_GREEN);
                squarePanel.setBorder(null);
            }
        }

        if (lastFrom != null) {
            squares[lastFrom.getRow()][lastFrom.getColumn()].setBorder(BORDER_LASTMOVE);
        }
        if (lastTo != null) {
            squares[lastTo.getRow()][lastTo.getColumn()].setBorder(BORDER_LASTMOVE);
        }

        if (selected != null) {
            squares[selected.getRow()][selected.getColumn()].setBorder(BORDER_SELECTED);
            for (Position d : legalForSelected) {
                squares[d.getRow()][d.getColumn()].setBorder(BORDER_LEGAL);
            }
        }

        int iconSize = computeSquareIconSize();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                JPanel squarePanel = squares[r][c];
                squarePanel.removeAll();

                Piece p = game.board().get(new Position(r, c));
                if (p != null) {
                    JLabel pieceLabel = new JLabel();
                    pieceLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    pieceLabel.setForeground(p.isWhite() ? WHITE_PIECE_COLOR : BLACK_PIECE_COLOR);
                    pieceLabel.setText(toUnicode(p.getSymbol(), p.isWhite()));
                    pieceLabel.setFont(new Font("Segoe UI Symbol", Font.PLAIN, (int) (iconSize * 0.85)));
                    squarePanel.add(pieceLabel);
                }
            }
        }

        boardPanel.revalidate();
        boardPanel.repaint();

        if (game.isGameOver()) {
            status.setText("Fim de Jogo!");
        } else if (!isGameActive) {
            status.setText("Faça o primeiro movimento para iniciar");
        } else {
            String side = game.whiteToMove() ? "Brancas" : "Laranjas";
            String chk = game.inCheck(game.whiteToMove()) ? " — Xeque!" : "";
            if (aiThinking && !game.whiteToMove()) {
                status.setText("Vez: Laranjas — IA pensando...");
            } else {
                status.setText("Vez: " + side + chk);
            }
        }

        historyModel.clear();
        var hist = game.history();
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < hist.size(); i++) {
            if (i % 2 == 0) {
                line = new StringBuilder();
                line.append((i / 2) + 1).append(". ");
            }
            line.append(hist.get(i)).append(" ");
            if (i % 2 == 1 || i == hist.size() - 1) {
                historyModel.addElement(line.toString().trim());
            }
        }
        if (!historyModel.isEmpty()) {
            historyList.setSelectedIndex(historyModel.getSize() - 1);
            historyList.ensureIndexIsVisible(historyList.getSelectedIndex());
        }
    }

    private void maybeAnnounceEnd() {
        if (!game.isGameOver()) {
            return;
        }
        isGameActive = false;
        if (gameTimer != null) {
            gameTimer.stop();
        }

        String msg = game.inCheck(game.whiteToMove())
                ? "Xeque-mate! " + (game.whiteToMove() ? "Laranjas venceram." : "Brancas venceram.")
                : "Empate por afogamento";

        showModernEndgameDialog("Fim de Jogo", msg);
    }

    private void showModernEndgameDialog(String title, String message) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setUndecorated(true);
        dialog.setSize(300, 150);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(COMPONENT_BG);
        mainPanel.setBorder(new LineBorder(BORDER_COLOR, 2));

        JLabel messageLabel = new JLabel(message);
        messageLabel.setFont(DIALOG_FONT);
        messageLabel.setForeground(ACCENT_WHITE);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        messageLabel.setBorder(new EmptyBorder(20, 20, 10, 20));
        mainPanel.add(messageLabel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel.setBackground(COMPONENT_BG);
        buttonPanel.setBorder(new EmptyBorder(0, 0, 15, 0));
        JButton okButton = createStyledButton("OK", BUTTON_GREEN, BG_COLOR);
        okButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(okButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private String toUnicode(String sym, boolean white) {
        return switch (sym) {
            case "K" ->
                white ? "\u2654" : "\u265A";
            case "Q" ->
                white ? "\u2655" : "\u265B";
            case "R" ->
                white ? "\u2656" : "\u265C";
            case "B" ->
                white ? "\u2657" : "\u265D";
            case "N" ->
                white ? "\u2658" : "\u265E";
            case "P" ->
                white ? "\u2659" : "\u265F";
            default ->
                "";
        };
    }

    private int computeSquareIconSize() {
        if (squares[0][0] == null) {
            return 64;
        }
        int w = squares[0][0].getWidth();
        int h = squares[0][0].getHeight();
        int side = Math.min(w, h);
        return (side <= 1) ? 64 : Math.max(24, side - 10);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChessGUI::new);
    }
}
