package com.example.proyectoaeropuerto;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class GraphView extends View {

    // --- Interfaz para el listener de toque largo ---
    public interface OnNodeLongClickListener {
        void onNodeLongClick(Aeropuerto aeropuerto);
    }
    private OnNodeLongClickListener longClickListener;

    // --- Pinceles para dibujar ---
    private Paint nodePaint, textPaint, linePaint, arrowPaint, weightTextPaint;
    private Paint highlightNodePaint, highlightLinePaint, highlightArrowPaint;
    private Paint routeNodePaint, routeLinePaint, routeArrowPaint;

    // --- Datos del grafo ---
    private LinkedList<Aeropuerto> aeropuertos = new LinkedList<>();
    private List<Vuelo> vuelos = new LinkedList<>();
    private LinkedList<Aeropuerto> rutaResaltada = new LinkedList<>();
    private final Map<String, Path> flightPaths = new HashMap<>();

    // --- Estado de la Interacción ---
    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;
    private float scaleFactor = 0.5f;
    private float pivotX, pivotY;
    private float offsetX = 0, offsetY = 0;
    private Aeropuerto selectedAeropuerto = null;

    public GraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        nodePaint = createPaint(R.color.graph_node, Paint.Style.FILL);
        textPaint = createTextPaint(R.color.graph_node_text, 32);
        linePaint = createPaint(R.color.graph_line, Paint.Style.STROKE, 5);
        arrowPaint = createPaint(R.color.graph_line, Paint.Style.FILL_AND_STROKE);
        weightTextPaint = createTextPaint(R.color.text_secondary, 28);
        highlightNodePaint = createPaint(R.color.graph_node_highlight, Paint.Style.FILL);
        highlightLinePaint = createPaint(R.color.graph_line_highlight, Paint.Style.STROKE, 8);
        highlightArrowPaint = createPaint(R.color.graph_line_highlight, Paint.Style.FILL_AND_STROKE);
        routeNodePaint = createPaint(R.color.graph_node_highlight, Paint.Style.FILL);
        routeLinePaint = createPaint(R.color.graph_line_highlight, Paint.Style.STROKE, 8);
        routeArrowPaint = createPaint(R.color.graph_line_highlight, Paint.Style.FILL_AND_STROKE);

        scaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        gestureDetector = new GestureDetector(getContext(), new GestureListener());
    }
    public void setOnNodeLongClickListener(OnNodeLongClickListener listener) {
        this.longClickListener = listener;
    }

    private Paint createPaint(int colorResId, Paint.Style style, float strokeWidth) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(ContextCompat.getColor(getContext(), colorResId));
        paint.setStyle(style);
        paint.setStrokeWidth(strokeWidth);
        return paint;
    }
    private Paint createPaint(int colorResId, Paint.Style style) { return createPaint(colorResId, style, 1); }
    private Paint createTextPaint(int colorResId, float textSize) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(ContextCompat.getColor(getContext(), colorResId));
        paint.setTextSize(textSize);
        paint.setTextAlign(Paint.Align.CENTER);
        return paint;
    }

    public void setSistemaDeVuelos(SistemaDeVuelos sistema) {
        this.aeropuertos = new LinkedList<>(sistema.getTodosLosAeropuertos());
        this.vuelos = sistema.getTodosLosVuelos();
        positionNodesInCircle();
    }

    public void highlightRoute(Ruta ruta) {
        this.rutaResaltada = (ruta != null && ruta.getAeropuertos() != null) ? ruta.getAeropuertos() : new LinkedList<>();
        invalidate();
    }

    private void positionNodesInCircle() {
        post(() -> {
            int width = getWidth();
            int height = getHeight();
            if (width == 0 || height == 0 || aeropuertos.isEmpty()) return;

            float virtualWidth = width * 2;
            float virtualHeight = height * 2;
            float centerX = virtualWidth / 2f;
            float centerY = virtualHeight / 2f;
            float radius = Math.min(virtualWidth, virtualHeight) * 0.45f;

            int numNodos = aeropuertos.size();
            for (int i = 0; i < numNodos; i++) {
                Aeropuerto apt = aeropuertos.get(i);
                double angle = 2 * Math.PI * i / numNodos - (Math.PI / 2);
                apt.x = (float) (centerX + radius * Math.cos(angle));
                apt.y = (float) (centerY + radius * Math.sin(angle));
            }

            offsetX = (width - virtualWidth) / 2f;
            offsetY = (height - virtualHeight) / 2f;

            calculateFlightPaths();
            invalidate();
        });
    }

    private void calculateFlightPaths() {
        flightPaths.clear();
        Map<String, Boolean> bidirectionalCheck = new HashMap<>();
        for (Vuelo vuelo : vuelos) {
            Aeropuerto origen = vuelo.getOrigen();
            Aeropuerto destino = vuelo.getDestino();
            String key = origen.getCodigoIATA() + "-" + destino.getCodigoIATA();
            String reverseKey = destino.getCodigoIATA() + "-" + origen.getCodigoIATA();
            float midX = (origen.x + destino.x) / 2;
            float midY = (origen.y + destino.y) / 2;
            float dx = destino.x - origen.x;
            float dy = destino.y - origen.y;
            float curvature = 0.15f;
            if (bidirectionalCheck.containsKey(reverseKey)) {
                curvature = 0.20f;
            }
            bidirectionalCheck.put(key, true);
            float controlX = midX - dy * curvature;
            float controlY = midY + dx * curvature;
            Path path = new Path();
            path.moveTo(origen.x, origen.y);
            path.quadTo(controlX, controlY, destino.x, destino.y);
            flightPaths.put(key, path);
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (aeropuertos.isEmpty()) return;
        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.scale(scaleFactor, scaleFactor, pivotX, pivotY);

        for (Vuelo vuelo : vuelos) {
            boolean isInRoute = isVueloInRuta(vuelo);
            boolean isHighlighted = selectedAeropuerto != null && (vuelo.getOrigen() == selectedAeropuerto || vuelo.getDestino() == selectedAeropuerto);
            Paint currentLinePaint = isInRoute ? routeLinePaint : (isHighlighted ? highlightLinePaint : linePaint);
            Paint currentArrowPaint = isInRoute ? routeArrowPaint : (isHighlighted ? highlightArrowPaint : arrowPaint);
            String key = vuelo.getOrigen().getCodigoIATA() + "-" + vuelo.getDestino().getCodigoIATA();
            Path path = flightPaths.get(key);
            if (path != null) {
                canvas.drawPath(path, currentLinePaint);
                drawArrowOnPath(canvas, path, currentArrowPaint);
                drawWeightOnPath(canvas, vuelo, path, weightTextPaint);
            }
        }

        float nodeRadius = 60;
        for (Aeropuerto apt : aeropuertos) {
            Paint currentNodePaint = rutaResaltada.contains(apt) ? routeNodePaint : (apt == selectedAeropuerto ? highlightNodePaint : nodePaint);
            canvas.drawCircle(apt.x, apt.y, nodeRadius, currentNodePaint);
            float textY = apt.y - ((textPaint.descent() + textPaint.ascent()) / 2);
            canvas.drawText(apt.getCodigoIATA(), apt.x, textY, textPaint);
        }
        canvas.restore();
    }

    private boolean isVueloInRuta(Vuelo vuelo) {
        for (int i = 0; i < rutaResaltada.size() - 1; i++) {
            if (rutaResaltada.get(i).equals(vuelo.getOrigen()) && rutaResaltada.get(i + 1).equals(vuelo.getDestino())) {
                return true;
            }
        }
        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean retVal = scaleDetector.onTouchEvent(event);
        retVal = gestureDetector.onTouchEvent(event) || retVal;
        return retVal || super.onTouchEvent(event);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(0.1f, Math.min(5.0f, scaleFactor));
            pivotX = detector.getFocusX();
            pivotY = detector.getFocusY();
            invalidate();
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
            offsetX -= distanceX;
            offsetY -= distanceY;
            invalidate();
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            selectedAeropuerto = findAeropuertoAt(e.getX(), e.getY());
            rutaResaltada.clear();
            invalidate();
            return true;
        }

        // Detectar el toque largo ---
        @Override
        public void onLongPress(MotionEvent e) {
            Aeropuerto tappedAeropuerto = findAeropuertoAt(e.getX(), e.getY());
            if (tappedAeropuerto != null && longClickListener != null) {
                longClickListener.onNodeLongClick(tappedAeropuerto);
            }
        }
    }

    private Aeropuerto findAeropuertoAt(float screenX, float screenY) {
        float tapX = (screenX - offsetX) / scaleFactor;
        float tapY = (screenY - offsetY) / scaleFactor;
        for (Aeropuerto apt : aeropuertos) {
            float dx = tapX - apt.x;
            float dy = tapY - apt.y;
            if (dx * dx + dy * dy < 60 * 60) {
                return apt;
            }
        }
        return null;
    }

    private void drawArrowOnPath(Canvas canvas, Path path, Paint paint) {
        android.graphics.PathMeasure pm = new android.graphics.PathMeasure(path, false);
        float length = pm.getLength();
        float[] pos = new float[2];
        float[] tan = new float[2];
        float distance = length - 60;
        if (distance < 0) distance = length / 2;
        pm.getPosTan(distance, pos, tan);
        float angle = (float) Math.toDegrees(Math.atan2(tan[1], tan[0]));
        Path arrowPath = new Path();
        arrowPath.moveTo(pos[0], pos[1]);
        float arrowHeadLength = 35;
        arrowPath.lineTo((float)(pos[0] - arrowHeadLength * Math.cos(Math.toRadians(angle - 35))), (float)(pos[1] - arrowHeadLength * Math.sin(Math.toRadians(angle - 35))));
        arrowPath.moveTo(pos[0], pos[1]);
        arrowPath.lineTo((float)(pos[0] - arrowHeadLength * Math.cos(Math.toRadians(angle + 35))), (float)(pos[1] - arrowHeadLength * Math.sin(Math.toRadians(angle + 35))));
        canvas.drawPath(arrowPath, paint);
    }

    private void drawWeightOnPath(Canvas canvas, Vuelo vuelo, Path path, Paint paint) {
        android.graphics.PathMeasure pm = new android.graphics.PathMeasure(path, false);
        float[] pos = new float[2];
        float[] tan = new float[2];
        pm.getPosTan(pm.getLength() / 2, pos, tan);
        String weight = String.valueOf(vuelo.getDistancia());
        float angle = (float) Math.toDegrees(Math.atan2(tan[1], tan[0]));
        canvas.save();
        canvas.translate(pos[0], pos[1]);
        canvas.rotate(angle > 90 || angle < -90 ? angle + 180 : angle); // Evitar texto invertido
        canvas.drawText(weight, 0, -15, paint);
        canvas.restore();
    }
}
