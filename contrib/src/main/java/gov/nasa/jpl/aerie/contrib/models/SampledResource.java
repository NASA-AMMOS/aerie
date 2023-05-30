package gov.nasa.jpl.aerie.contrib.models;

import gov.nasa.jpl.aerie.merlin.framework.resources.discrete.DiscreteResource;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.*;

/**
 * Simple resource that samples arbitrarily many existing resources/values at a specified rate (default rate is once per
 * second).
 */
public class SampledResource<T> implements DiscreteResource<T> {
  private final Register<T> result;
  private final Supplier<T> sampler;
  private double rate;

  /**
   * Constructor that does not require caller to specify a rate and therefore assumes a sample
   * rate of 1 sample per second.
   */
  public SampledResource(final Supplier<T> sampler, final UnaryOperator<T> duplicator) {
    this.result = Register.create(sampler.get(), duplicator);
    this.sampler = Objects.requireNonNull(sampler);
    this.rate = 1.0;
    spawn(this::takeSamples);
  }

  /**
   * Constructor that requires caller to specify an initial sample rate
   */
  public SampledResource(final Supplier<T> sampler, final UnaryOperator<T> duplicator, final double rate) {
    this.result = Register.create(sampler.get(), duplicator);
    this.sampler = Objects.requireNonNull(sampler);
    this.rate = rate;
    spawn(this::takeSamples);
  }

  /**
   * Method that samples the supplied resource at the current specified rate
   */
  private void takeSamples() {
    while (true) {
      var sample = sampler.get();
      if (!result.get().equals(sample)) {
        result.set(sample);
      }
      delay( Duration.roundNearest(rate, Duration.SECONDS) );
    }
  }

  /**
   * Get current sample rate
   */
  public double getRate() { return rate; }

  /**
   * Method to adjust the specified rate of sampling. Note if takeSamples() is currently waiting, the
   * new rate will not take effect until after the current wait cycle.
   */
  public void setRate(final double newRate ) { rate = newRate; }

  @Override
  public T getDynamics() {
    return this.result.getDynamics();
  }

  @Deprecated
  @Override
  public boolean equals(final Object obj) {
    return super.equals(obj);
  }
}
