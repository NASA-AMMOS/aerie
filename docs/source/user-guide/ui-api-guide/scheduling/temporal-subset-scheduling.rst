==========================
Temporal Subset Scheduling
==========================

Sometimes, it may be desirable to limit the action of the scheduler to a certain time range. This can be accomplished using :doc:`Global Scheduling Conditions <./scheduling-conditions>`.

First, you'll need to define a numeric resource in your mission model that represents time - for example, you could use a `Clock <https://github.com/NASA-AMMOS/aerie/blob/0840e98dce22b7e38b7425082a617ffbf968ce97/contrib/src/main/java/gov/nasa/jpl/aerie/contrib/models/Clock.java>`_.

.. code-block:: java

   Mission(Registrar registrar, Instant planStart) {
      final var clock = new Clock(planStart);
      registrar.realResource("/clock", clock.ticks);
   }

Then you can write a global scheduling condition based on that resource. This example says that the scheduler may only place activities between 8 and 12 days after the start of the plan.

.. code-block:: typescript

   export default (): GlobalSchedulingCondition =>
     GlobalSchedulingCondition.scheduleActivitiesOnlyWhen(
       Real.Resource('/clock').greaterThan(Temporal.Duration.from({ days: 8 }).total("milliseconds"))
       .and(Real.Resource('/clock').lessThan(Temporal.Duration.from({ days: 12 }).total("milliseconds"))))
