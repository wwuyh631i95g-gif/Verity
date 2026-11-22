package app.verity.blocktetris;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;

final class BlockTetrisView extends View {
    private static final int COLS = 10, ROWS = 20;
    private static final int[][][] SHAPES = {
        {{0,1},{1,1},{2,1},{3,1}},
        {{0,0},{0,1},{1,1},{2,1}},
        {{2,0},{0,1},{1,1},{2,1}},
        {{1,0},{2,0},{1,1},{2,1}},
        {{1,0},{2,0},{0,1},{1,1}},
        {{1,0},{0,1},{1,1},{2,1}},
        {{0,0},{1,0},{1,1},{2,1}}
    };
    private static final int[] COLORS = {
        0xFF35C6E6, 0xFF4E7DEB, 0xFFFF963F, 0xFFFFD64A,
        0xFF57C978, 0xFFAA78E8, 0xFFE96BAE
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final int[][] board = new int[ROWS][COLS];
    private final RectF[] buttons = new RectF[5];
    private Piece current;
    private int nextType, score, lines, level = 1;
    private boolean gameOver, paused = true;
    private float cell, boardLeft, boardTop, downX, downY;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (!paused && !gameOver) {
                stepDown();
                invalidate();
                handler.postDelayed(this, Math.max(120, 620 - (level - 1) * 38));
            }
        }
    };

    BlockTetrisView(Context context) {
        super(context);
        setFocusable(true);
        for (int i = 0; i < buttons.length; i++) buttons[i] = new RectF();
        resetGame();
    }

    void resumeGame() {
        paused = false;
        handler.removeCallbacks(ticker);
        handler.postDelayed(ticker, 450);
    }

    void pauseGame() {
        paused = true;
        handler.removeCallbacks(ticker);
    }

    @Override protected void onDetachedFromWindow() {
        pauseGame();
        super.onDetachedFromWindow();
    }

    private void resetGame() {
        for (int y = 0; y < ROWS; y++) for (int x = 0; x < COLS; x++) board[y][x] = 0;
        score = 0; lines = 0; level = 1; gameOver = false;
        nextType = random.nextInt(SHAPES.length);
        spawnPiece();
    }

    private void spawnPiece() {
        current = new Piece(nextType, 3, 0);
        nextType = random.nextInt(SHAPES.length);
        if (collides(current, current.x, current.y, current.rotation)) gameOver = true;
    }

    private int[][] cells(int type, int rotation) {
        int[][] source = SHAPES[type];
        int[][] result = new int[source.length][2];
        for (int i = 0; i < source.length; i++) {
            int x = source[i][0], y = source[i][1];
            for (int r = 0; r < rotation; r++) {
                int nx = 3 - y;
                y = x;
                x = nx;
            }
            result[i][0] = x;
            result[i][1] = y;
        }
        return result;
    }

    private boolean collides(Piece piece, int px, int py, int rotation) {
        for (int[] c : cells(piece.type, rotation)) {
            int x = px + c[0], y = py + c[1];
            if (x < 0 || x >= COLS || y >= ROWS) return true;
            if (y >= 0 && board[y][x] != 0) return true;
        }
        return false;
    }

    private void move(int dx) {
        if (gameOver || paused) return;
        if (!collides(current, current.x + dx, current.y, current.rotation)) current.x += dx;
        invalidate();
    }

    private void rotate() {
        if (gameOver || paused) return;
        int target = (current.rotation + 1) & 3;
        if (!collides(current, current.x, current.y, target)) {
            current.rotation = target;
        } else if (!collides(current, current.x - 1, current.y, target)) {
            current.x--; current.rotation = target;
        } else if (!collides(current, current.x + 1, current.y, target)) {
            current.x++; current.rotation = target;
        }
        invalidate();
    }

    private void softDrop() {
        if (gameOver || paused) return;
        if (!collides(current, current.x, current.y + 1, current.rotation)) {
            current.y++;
            score++;
        } else lockPiece();
        invalidate();
    }

    private void hardDrop() {
        if (gameOver || paused) return;
        int distance = 0;
        while (!collides(current, current.x, current.y + 1, current.rotation)) {
            current.y++;
            distance++;
        }
        score += distance * 2;
        lockPiece();
        invalidate();
    }

    private void stepDown() {
        if (!collides(current, current.x, current.y + 1, current.rotation)) current.y++;
        else lockPiece();
    }

    private void lockPiece() {
        for (int[] c : cells(current.type, current.rotation)) {
            int x = current.x + c[0], y = current.y + c[1];
            if (y < 0) { gameOver = true; return; }
            if (y < ROWS && x >= 0 && x < COLS) board[y][x] = current.type + 1;
        }
        clearLines();
        if (!gameOver) spawnPiece();
    }

    private void clearLines() {
        int cleared = 0;
        for (int y = ROWS - 1; y >= 0; y--) {
            boolean full = true;
            for (int x = 0; x < COLS; x++) if (board[y][x] == 0) { full = false; break; }
            if (full) {
                cleared++;
                for (int yy = y; yy > 0; yy--) System.arraycopy(board[yy - 1], 0, board[yy], 0, COLS);
                for (int x = 0; x < COLS; x++) board[0][x] = 0;
                y++;
            }
        }
        if (cleared > 0) {
            int[] points = {0, 100, 300, 500, 800};
            score += points[Math.min(4, cleared)] * level;
            lines += cleared;
            level = 1 + lines / 10;
        }
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float width = getWidth(), height = getHeight();
        canvas.drawColor(0xFF100C25);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSize(23 * density);
        paint.setColor(0xFFFFD84A);
        canvas.drawText("BLOCK TETRIS", width / 2, 31 * density, paint);
        paint.setFakeBoldText(false);
        paint.setTextSize(11 * density);
        paint.setColor(0xFFE4DFF7);
        canvas.drawText("СЧЁТ  " + score + "      ЛИНИИ  " + lines + "      УРОВЕНЬ  " + level,
                width / 2, 56 * density, paint);

        cell = Math.min((width - 26 * density) / COLS, (height - 162 * density) / ROWS);
        cell = Math.max(8 * density, cell);
        boardLeft = (width - cell * COLS) / 2;
        boardTop = 70 * density;

        RectF frame = new RectF(boardLeft - 3 * density, boardTop - 3 * density,
                boardLeft + cell * COLS + 3 * density, boardTop + cell * ROWS + 3 * density);
        paint.setColor(0xFF2B2149);
        canvas.drawRoundRect(frame, 9 * density, 9 * density, paint);

        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLS; x++) {
                float left = boardLeft + x * cell, top = boardTop + y * cell;
                if (board[y][x] == 0) {
                    paint.setColor(0xFF19142F);
                    canvas.drawRoundRect(new RectF(left + 1, top + 1, left + cell - 1, top + cell - 1),
                            3 * density, 3 * density, paint);
                } else {
                    drawBlock(canvas, left, top, cell, COLORS[board[y][x] - 1], density, board[y][x] - 1);
                }
            }
        }
        if (!gameOver && current != null) {
            for (int[] c : cells(current.type, current.rotation)) {
                float left = boardLeft + (current.x + c[0]) * cell;
                float top = boardTop + (current.y + c[1]) * cell;
                drawBlock(canvas, left, top, cell, COLORS[current.type], density, current.type);
            }
        }
        drawButtons(canvas, density);

        if (gameOver) {
            paint.setColor(0xCC090714);
            canvas.drawRect(0, 0, width, height, paint);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(0xFFFFD84A);
            paint.setTextSize(28 * density);
            paint.setFakeBoldText(true);
            canvas.drawText("ИГРА ОКОНЧЕНА", width / 2, height / 2 - 12 * density, paint);
            paint.setFakeBoldText(false);
            paint.setTextSize(14 * density);
            paint.setColor(Color.WHITE);
            canvas.drawText("Нажми, чтобы начать заново", width / 2, height / 2 + 18 * density, paint);
        }
    }

    private void drawBlock(Canvas canvas, float x, float y, float size, int color, float d, int mood) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        RectF block = new RectF(x + 1.5f*d, y + 1.5f*d, x + size - 1.5f*d, y + size - 1.5f*d);
        canvas.drawRoundRect(block, 4*d, 4*d, paint);
        paint.setColor(0x55FFFFFF);
        canvas.drawRoundRect(new RectF(block.left + 2*d, block.top + 2*d,
                block.right - 2*d, block.top + size * .18f), 3*d, 3*d, paint);
        paint.setColor(0x55000000);
        canvas.drawRect(block.left + 2*d, block.bottom - size*.11f, block.right - 2*d, block.bottom - 2*d, paint);
        float cx = block.centerX(), cy = block.centerY();
        paint.setColor(0xFF211735);
        float eye = Math.max(1.2f*d, size*.055f);
        canvas.drawCircle(cx - size*.18f, cy - size*.08f, eye, paint);
        canvas.drawCircle(cx + size*.18f, cy - size*.08f, eye, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1.0f*d, size*.04f));
        RectF mouth = new RectF(cx-size*.16f, cy-size*.01f, cx+size*.16f, cy+size*.24f);
        canvas.drawArc(mouth, mood % 3 == 0 ? 15 : 195, 150, false, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawButtons(Canvas canvas, float d) {
        float top = getHeight() - 65*d, left = 8*d, gap = 5*d;
        float buttonWidth = (getWidth() - 16*d - gap*4) / 5f, buttonHeight = 48*d;
        String[] labels = {"◀", "↻", "▶", "▼", "DROP"};
        for (int i = 0; i < buttons.length; i++) {
            float x = left + i * (buttonWidth + gap);
            buttons[i].set(x, top, x + buttonWidth, top + buttonHeight);
            paint.setColor(0xFF393052);
            canvas.drawRoundRect(buttons[i], 11*d, 11*d, paint);
            paint.setColor(i == 4 ? 0xFFFFD84A : 0xFFF5F1FF);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize((i == 4 ? 10 : 21)*d);
            paint.setFakeBoldText(i == 4);
            canvas.drawText(labels[i], buttons[i].centerX(), buttons[i].centerY() + 7*d, paint);
        }
        paint.setFakeBoldText(false);
        paint.setTextSize(9*d);
        paint.setColor(0xFFBFB7D5);
        canvas.drawText("Свайп вверх — поворот · вниз — ускорение", getWidth()/2, top - 7*d, paint);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            downX = event.getX();
            downY = event.getY();
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) return true;
        if (event.getActionMasked() != MotionEvent.ACTION_UP) return true;

        float x = event.getX(), y = event.getY();
        if (gameOver) {
            resetGame();
            resumeGame();
            invalidate();
            return true;
        }
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i].contains(x, y)) {
                switch (i) {
                    case 0: move(-1); break;
                    case 1: rotate(); break;
                    case 2: move(1); break;
                    case 3: softDrop(); break;
                    case 4: hardDrop(); break;
                }
                return true;
            }
        }
        if (y >= boardTop && y <= boardTop + ROWS * cell) {
            float dx = x - downX, dy = y - downY;
            if (Math.abs(dx) + Math.abs(dy) > 28) {
                if (Math.abs(dx) > Math.abs(dy)) move(dx < 0 ? -1 : 1);
                else if (dy < 0) rotate();
                else softDrop();
            } else {
                move(x < getWidth()/2f ? -1 : 1);
            }
        }
        return true;
    }

    private static final class Piece {
        final int type;
        int x, y, rotation;
        Piece(int type, int x, int y) { this.type = type; this.x = x; this.y = y; }
    }
}
