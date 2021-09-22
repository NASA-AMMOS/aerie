package gov.nasa.jpl.aerie.scheduler;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRendererState;
import org.jfree.chart.renderer.category.GanttRenderer;
import org.jfree.chart.text.TextUtils;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.IntervalCategoryDataset;
import org.jfree.data.gantt.GanttCategoryDataset;
import org.jfree.data.gantt.Task;
import org.jfree.data.gantt.TaskSeries;
import org.jfree.data.gantt.TaskSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Class creating a JfreeChart Gantt visualization of a Plan
 */
public class PlanVisualizer extends JFrame {

    /**
     * Visualize a plan with the JfreeChart library
     * Horizon marker (activity type "HorizonMarker") and windows activities (activity type "Window") are treated specifically
     * @param plan the activity plan
     */
    public static void visualizePlan(Plan plan) {

        SwingUtilities.invokeLater(
                () -> {
                    PlanVisualizer p = new PlanVisualizer(plan, "Plan");

                });

    }
    /**
     * Creates a JfreeChart visualization from a dataset
     * @param title
     * title of the window
     * dataset
     */
    private PlanVisualizer(Plan plan, String title) {
        super(title);
        IntervalCategoryDataset dataset = convertPlanToDataset(plan);

        // Create chart
        JFreeChart chart = ChartFactory.createGanttChart(
                "Plan", // Chart title
                "Activity types", // X-Axis Label
                "Timeline", // Y-Axis Label
                dataset);

        CategoryPlot plot = (CategoryPlot) chart.getPlot();

        MyGanttRenderer renderer = new MyGanttRenderer();
        plot.setRenderer(renderer);
        renderer.setDefaultItemLabelsVisible(true);
        renderer.setDefaultItemLabelPaint(Color.BLACK);
        renderer.setDefaultPositiveItemLabelPosition(new ItemLabelPosition(
                ItemLabelAnchor.INSIDE6, TextAnchor.BOTTOM_CENTER));
        ChartPanel panel = new ChartPanel(chart);
        panel.setMouseWheelEnabled(true);
        setContentPane(panel);
        this.setSize(800, 400);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setVisible(true);
    }



    /**
     * Create as much TaskSeries as needed to visualize all possibly overlapping windows and inserts them in the dataset
     * @param windows all the windows activities
     * @param dataset the dataset in which the series are inserted
     * @param minStartTime the minimum start time of the activities
     * @param maxEndTime the maximum end time of the activities
     */
    private void createWindowDataset(List<ActivityInstance> windows, TaskSeriesCollection dataset, Time minStartTime, Time maxEndTime){

        List<List<ActivityInstance>> nonOverlappingWindows = new ArrayList<List<ActivityInstance>>();
        nonOverlappingWindows.add(new ArrayList<ActivityInstance>());

        for(var act : windows){
            boolean found = false;
            Range<Time> win = new Range<Time>(act.getStartTime(), act.getStartTime().plus(act.getDuration()));
            for(var listOfWin : nonOverlappingWindows){
                boolean next = false;
                for(var actInList : listOfWin) {
                    Range<Time> actToCompare = new Range<Time>(actInList.getStartTime(), actInList.getStartTime().plus(actInList.getDuration()));
                    if(win.intersect(actToCompare) != null){
                        next = true;
                        break;
                    }
                }
                if(!next){
                    //no act were overlapped in this list, add the activity to it
                    found = true;
                    listOfWin.add(act);
                    break;
                }

            }
            //if it has overlapped all the existing list, create a new one
            if(!found){
                ArrayList<ActivityInstance> newList = new ArrayList<ActivityInstance>();
                newList.add(act);
                nonOverlappingWindows.add(newList);
            }
        }

        int i = 0;
        for(var listNonOver : nonOverlappingWindows){
            TaskSeries serie = getSerieAndSubtasksFromListActs(windowTypeAct+i, listNonOver, minStartTime, maxEndTime);
            dataset.add(serie);
            i++;
        }


    }

    /**
     * Produces a TaskSeries fron a list of activity instances
     * @param nameSerie the name of the serie (the type of activity)
     * @param activities the list of activity instances
     * @param minStartTime the minimum start time of the activities
     * @param maxEndTime the maximum end time of the activities
     * @return a TaskSeries object containing one big task which itself contains one subtask for each activities in the list
     */
    private TaskSeries getSerieAndSubtasksFromListActs(String nameSerie, List<ActivityInstance> activities, Time minStartTime, Time maxEndTime){
        TaskSeries serie = new TaskSeries(nameSerie);
        Task overallTask = new Task(nameSerie,Date.from(minStartTime.toInstant()), Date.from(maxEndTime.toInstant()));

        for(final var act : activities){
            String name;
            Duration dur;
            // To visualize instant events, a fictional 1 hour duration is set
            if(act.getDuration()==null) {
                name = act.getName() + " INSTANT EVENT ";
                dur = Duration.ofHours(1);
            }
            else {
                name = act.getName();
                dur = act.getDuration();
            }
            Task actst = new Task(name, Date.from(act.getStartTime().toInstant()), Date.from(act.getStartTime().plus(dur).toInstant()));
            overallTask.addSubtask(actst);
        }

        serie.add(overallTask);
        return serie;
    }

    /**
     * Converts a Plan into a dataset that is plottable by the Gantt JFreeChart Module
     * Horizon marker (activity type "HorizonMarker") and windows activities (activity type "Window") are treated specifically
     * @param plan a Plan
     * @return a gantt type dataset
     */
    private IntervalCategoryDataset convertPlanToDataset(Plan plan){
        TaskSeriesCollection dataset = new TaskSeriesCollection();
        List<ActivityInstance> actsbystarttime = plan.getActivitiesByTime();
        ActivityInstance firstAct = actsbystarttime.get(0);
        Time minStartTime = firstAct.getStartTime();
        Time maxEndTime = actsbystarttime.get(actsbystarttime.size()-1).getStartTime();

        //assumption : all non-windows activity types do not have overlapping instances

        for(final var typeAndActs : plan.getActivitiesByType().entrySet()){
            // create task for that activity type

            if(typeAndActs.getKey().equals(horizonMarkerTypeAct)){
                continue;
            } else if (typeAndActs.getKey().equals(windowTypeAct)){
                createWindowDataset(typeAndActs.getValue(), dataset, minStartTime, maxEndTime);
                continue;
            }

            TaskSeries serie = getSerieAndSubtasksFromListActs(typeAndActs.getKey(), typeAndActs.getValue(),  minStartTime,  maxEndTime);

            dataset.add(serie);
        }

        return dataset;

    }



    private static String windowTypeAct = "Window";
    private static String horizonMarkerTypeAct = "HorizonMarker";
    private static final long serialVersionUID = 1L;


    /**
     * Custom GanttRenderer for adding labels and tooltips to subtasks
     */
    private class MyGanttRenderer extends GanttRenderer {
        private transient Paint completePaint;
        private transient Paint incompletePaint;
        private double startPercent;
        private double endPercent;

        public MyGanttRenderer() {
            super();
            setIncludeBaseInRange(false);
            this.completePaint = Color.green;
            this.incompletePaint = Color.red;
            this.startPercent = 0.35;
            this.endPercent = 0.65;
        }
        static final long serialVersionUID = 42L;

        protected void drawTasks(Graphics2D g2,
                                 CategoryItemRendererState state,
                                 Rectangle2D dataArea,
                                 CategoryPlot plot,
                                 CategoryAxis domainAxis,
                                 ValueAxis rangeAxis,
                                 GanttCategoryDataset dataset,
                                 int row,
                                 int column) {

            int count = dataset.getSubIntervalCount(row, column);
            if (count == 0) {
                drawTask(g2, state, dataArea, plot, domainAxis, rangeAxis,
                        dataset, row, column);
            }

            for (int subinterval = 0; subinterval < count; subinterval++) {

                RectangleEdge rangeAxisLocation = plot.getRangeAxisEdge();

                // value 0
                Number value0 = dataset.getStartValue(row, column, subinterval);
                if (value0 == null) {
                    return;
                }
                double translatedValue0 = rangeAxis.valueToJava2D(
                        value0.doubleValue(), dataArea, rangeAxisLocation);

                // value 1
                Number value1 = dataset.getEndValue(row, column, subinterval);
                if (value1 == null) {
                    return;
                }
                double translatedValue1 = rangeAxis.valueToJava2D(
                        value1.doubleValue(), dataArea, rangeAxisLocation);

                if (translatedValue1 < translatedValue0) {
                    double temp = translatedValue1;
                    translatedValue1 = translatedValue0;
                    translatedValue0 = temp;
                }

                double rectStart = calculateBarW0(plot, plot.getOrientation(),
                        dataArea, domainAxis, state, row, column);
                double rectLength = Math.abs(translatedValue1 - translatedValue0);
                double rectBreadth = state.getBarWidth();

                // DRAW THE BARS...
                Rectangle2D bar = null;

                if (plot.getOrientation() == PlotOrientation.HORIZONTAL) {
                    bar = new Rectangle2D.Double(translatedValue0, rectStart,
                            rectLength, rectBreadth);
                }
                else if (plot.getOrientation() == PlotOrientation.VERTICAL) {
                    bar = new Rectangle2D.Double(rectStart, translatedValue0,
                            rectBreadth, rectLength);
                }

                Rectangle2D completeBar = null;
                Rectangle2D incompleteBar = null;
                Number percent = dataset.getPercentComplete(row, column,
                        subinterval);
                double start = getStartPercent();
                double end = getEndPercent();
                if (percent != null) {
                    double p = percent.doubleValue();
                    if (plot.getOrientation() == PlotOrientation.HORIZONTAL) {
                        completeBar = new Rectangle2D.Double(translatedValue0,
                                rectStart + start * rectBreadth, rectLength * p,
                                rectBreadth * (end - start));
                        incompleteBar = new Rectangle2D.Double(translatedValue0
                                + rectLength * p, rectStart + start * rectBreadth,
                                rectLength * (1 - p), rectBreadth * (end - start));
                    }
                    else if (plot.getOrientation() == PlotOrientation.VERTICAL) {
                        completeBar = new Rectangle2D.Double(rectStart + start
                                * rectBreadth, translatedValue0 + rectLength
                                * (1 - p), rectBreadth * (end - start),
                                rectLength * p);
                        incompleteBar = new Rectangle2D.Double(rectStart + start
                                * rectBreadth, translatedValue0, rectBreadth
                                * (end - start), rectLength * (1 - p));
                    }

                }

                Paint seriesPaint = getItemPaint(row, column);
                g2.setPaint(seriesPaint);
                g2.fill(bar);

                if (completeBar != null) {
                    g2.setPaint(getCompletePaint());
                    g2.fill(completeBar);
                }
                if (incompleteBar != null) {
                    g2.setPaint(getIncompletePaint());
                    g2.fill(incompleteBar);
                }
                if (isDrawBarOutline()
                        && state.getBarWidth() > BAR_OUTLINE_WIDTH_THRESHOLD) {
                    g2.setStroke(getItemStroke(row, column));
                    g2.setPaint(getItemOutlinePaint(row, column));
                    g2.draw(bar);
                }

                MyCategoryItemLabelGenerator generator = new MyCategoryItemLabelGenerator();
                if (isItemLabelVisible(row, column)) {
                    drawItemLabel(g2, dataset, row, column,subinterval, plot, generator, bar, false);
                }

                // collect entity and tool tip information...
                if (state.getInfo() != null) {
                    EntityCollection entities = state.getEntityCollection();
                    if (entities != null) {
                        String tip;

                        MyToolTipGenerator ttg = new MyToolTipGenerator();

                        tip = ttg.generateToolTip(dataset, row, column, subinterval);
                        String url = null;
                        if (getItemURLGenerator(row, column) != null) {
                            url = getItemURLGenerator(row, column).generateURL(
                                    dataset, row, column);
                        }
                        CategoryItemEntity entity = new CategoryItemEntity(
                                bar, tip, url, dataset, dataset.getRowKey(row),
                                dataset.getColumnKey(column));
                        entities.add(entity);
                    }
                }
            }
        }

        private void drawItemLabel(Graphics2D g2,
                                   CategoryDataset data,
                                   int row,
                                   int column,
                                   int sub,
                                   CategoryPlot plot,
                                   MyCategoryItemLabelGenerator generator,
                                   Rectangle2D bar,
                                   boolean negative) {

            //String label =
            String label = generator.generateLabel(data, row, column, sub);
            if (label == null) {
                return;  // nothing to do
            }

            Font labelFont = getItemLabelFont(row, column);
            g2.setFont(labelFont);
            Paint paint = getItemLabelPaint(row, column);
            g2.setPaint(paint);

            // find out where to place the label...
            ItemLabelPosition position;
            if (!negative) {
                position = getPositiveItemLabelPosition(row, column);
            }
            else {
                position = getNegativeItemLabelPosition(row, column);
            }

            // work out the label anchor point...
            Point2D anchorPoint = calculateLabelAnchorPoint(position.getItemLabelAnchor(), bar, plot.getOrientation());



            if (isInternalAnchor(position.getItemLabelAnchor())) {
                Shape bounds = TextUtils.calculateRotatedStringBounds(label,
                        g2, (float) anchorPoint.getX(), (float) anchorPoint.getY(),
                        position.getTextAnchor(), position.getAngle(),
                        position.getRotationAnchor());

                if (bounds != null) {
                    if (!bar.contains(bounds.getBounds2D())) {
                        if (!negative) {
                            position = getPositiveItemLabelPositionFallback();
                        }
                        else {
                            position = getNegativeItemLabelPositionFallback();
                        }
                        if (position != null) {
                            anchorPoint = calculateLabelAnchorPoint(
                                    position.getItemLabelAnchor(), bar,
                                    plot.getOrientation());
                        }
                    }
                }

            }

            if (position != null) {
                TextUtils.drawRotatedString(label, g2,
                        (float) anchorPoint.getX(), (float) anchorPoint.getY(),
                        position.getTextAnchor(), position.getAngle(),
                        position.getRotationAnchor());
            }
        }


        /**
         * Returns <code>true</code> if the specified anchor point is inside a bar.
         *
         * @param anchor
         *            the anchor point.
         *
         * @return A boolean.
         */
        private boolean isInternalAnchor(ItemLabelAnchor anchor) {
            return anchor == ItemLabelAnchor.CENTER
                    || anchor == ItemLabelAnchor.INSIDE1
                    || anchor == ItemLabelAnchor.INSIDE2
                    || anchor == ItemLabelAnchor.INSIDE3
                    || anchor == ItemLabelAnchor.INSIDE4
                    || anchor == ItemLabelAnchor.INSIDE5
                    || anchor == ItemLabelAnchor.INSIDE6
                    || anchor == ItemLabelAnchor.INSIDE7
                    || anchor == ItemLabelAnchor.INSIDE8
                    || anchor == ItemLabelAnchor.INSIDE9
                    || anchor == ItemLabelAnchor.INSIDE10
                    || anchor == ItemLabelAnchor.INSIDE11
                    || anchor == ItemLabelAnchor.INSIDE12;
        }


        private Point2D calculateLabelAnchorPoint(ItemLabelAnchor anchor,
                                                  Rectangle2D bar, PlotOrientation orientation) {

            Point2D result = null;
            double offset = getItemLabelAnchorOffset();
            double x0 = bar.getX() - offset;
            double x1 = bar.getX();
            double x2 = bar.getX() + offset;
            double x3 = bar.getCenterX();
            double x4 = bar.getMaxX() - offset;
            double x5 = bar.getMaxX();
            double x6 = bar.getMaxX() + offset;

            double y0 = bar.getMaxY() + offset;
            double y1 = bar.getMaxY();
            double y2 = bar.getMaxY() - offset;
            double y3 = bar.getCenterY();
            double y4 = bar.getMinY() + offset;
            double y5 = bar.getMinY();
            double y6 = bar.getMinY() - offset;

            if (anchor == ItemLabelAnchor.CENTER) {
                result = new Point2D.Double(x3, y3);
            }
            else if (anchor == ItemLabelAnchor.INSIDE1) {
                result = new Point2D.Double(x4, y4);
            }
            else if (anchor == ItemLabelAnchor.INSIDE2) {
                result = new Point2D.Double(x4, y4);
            }
            else if (anchor == ItemLabelAnchor.INSIDE3) {
                result = new Point2D.Double(x4, y3);
            }
            else if (anchor == ItemLabelAnchor.INSIDE4) {
                result = new Point2D.Double(x4, y2);
            }
            else if (anchor == ItemLabelAnchor.INSIDE5) {
                result = new Point2D.Double(x4, y2);
            }
            else if (anchor == ItemLabelAnchor.INSIDE6) {
                result = new Point2D.Double(x3, y2);
            }
            else if (anchor == ItemLabelAnchor.INSIDE7) {
                result = new Point2D.Double(x2, y2);
            }
            else if (anchor == ItemLabelAnchor.INSIDE8) {
                result = new Point2D.Double(x2, y2);
            }
            else if (anchor == ItemLabelAnchor.INSIDE9) {
                result = new Point2D.Double(x2, y3);
            }
            else if (anchor == ItemLabelAnchor.INSIDE10) {
                result = new Point2D.Double(x2, y4);
            }
            else if (anchor == ItemLabelAnchor.INSIDE11) {
                result = new Point2D.Double(x2, y4);
            }
            else if (anchor == ItemLabelAnchor.INSIDE12) {
                result = new Point2D.Double(x3, y4);
            }
            else if (anchor == ItemLabelAnchor.OUTSIDE1) {
                result = new Point2D.Double(x5, y6);
            }
            else if (anchor == ItemLabelAnchor.OUTSIDE2) {
                result = new Point2D.Double(x6, y5);
            }
            else if (anchor == ItemLabelAnchor.OUTSIDE3) {
                result = new Point2D.Double(x6, y3);
            }
            else if (anchor == ItemLabelAnchor.OUTSIDE4) {
                result = new Point2D.Double(x6, y1);
            }
            else if (anchor == ItemLabelAnchor.OUTSIDE5) {
                result = new Point2D.Double(x5, y0);
            }
            else if (anchor == ItemLabelAnchor.OUTSIDE6) {
                result = new Point2D.Double(x3, y0);
            }
            else if (anchor == ItemLabelAnchor.OUTSIDE7) {
                result = new Point2D.Double(x1, y0);
            }
            else if (anchor == ItemLabelAnchor.OUTSIDE8) {
                result = new Point2D.Double(x0, y1);
            }
            else if (anchor == ItemLabelAnchor.OUTSIDE9) {
                result = new Point2D.Double(x0, y3);
            }
            else if (anchor == ItemLabelAnchor.OUTSIDE10) {
                result = new Point2D.Double(x0, y5);
            }
            else if (anchor == ItemLabelAnchor.OUTSIDE11) {
                result = new Point2D.Double(x1, y6);
            }
            else if (anchor == ItemLabelAnchor.OUTSIDE12) {
                result = new Point2D.Double(x3, y6);
            }

            return result;

        }
    }

    private class MyToolTipGenerator implements CategoryToolTipGenerator{
        public String generateToolTip(CategoryDataset data, int a, int b){
            return null;
        }
        @SuppressWarnings("unchecked")
        public String generateToolTip(CategoryDataset dataSet, int a, int b, int sub){
            TaskSeries series3 = (TaskSeries) dataSet.getRowKeys().get(a);
            List<Task> tasks = series3.getTasks(); // unchecked
            if(tasks.size()>b) {
                return tasks.get(b).getSubtask(sub).getDescription();
            }else{
                return tasks.get(0).getSubtask(sub).getDescription();
            }
        }

    }

    private class MyCategoryItemLabelGenerator implements CategoryItemLabelGenerator{
        HashMap<String, List<Task>> subTaskMap;

        public MyCategoryItemLabelGenerator() {
            subTaskMap = new HashMap<String, List<Task>>();
        }

        @SuppressWarnings("unchecked")
        public String generateLabel(CategoryDataset dataSet, int series, int categories, int sub) {
            TaskSeries series3 = (TaskSeries) dataSet.getRowKeys().get(series);
            List<Task> tasks = series3.getTasks(); // unchecked
            if(tasks.size()>categories) {
                return tasks.get(categories).getSubtask(sub).getDescription();
            }else{
                return tasks.get(0).getSubtask(sub).getDescription();
            }
        }


        public String generateColumnLabel(CategoryDataset dataset, int categories) {
            return dataset.getColumnKey(categories).toString();
        }

        @Override
        public String generateLabel(CategoryDataset categoryDataset, int i, int i1) {
            return null;
        }

        public String generateRowLabel(CategoryDataset dataset, int series) {
            return dataset.getRowKey(series).toString();
        }
    }


}
