package gov.nasa.jpl.ammos.mpsa.aerie.services.cli;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.PrintWriter;

import static org.junit.platform.engine.discovery.ClassNameFilter.includeClassNamePatterns;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

public class TestMain {
  public static void main(final String[] args) {
    final SummaryGeneratingListener listener = new SummaryGeneratingListener();
    final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
        .selectors(selectPackage("gov.nasa.jpl.ammos.mpsa.aerie.services.merlincli"))
        .filters(includeClassNamePatterns(".*Tests"))
        .build();

    final Launcher launcher = LauncherFactory.create();
    final TestPlan testPlan = launcher.discover(request);
    launcher.registerTestExecutionListeners(listener);
    launcher.execute(request);

    final TestExecutionSummary summary = listener.getSummary();
    summary.printTo(new PrintWriter(System.out));
    summary.printFailuresTo(new PrintWriter(System.out));
  }
}
