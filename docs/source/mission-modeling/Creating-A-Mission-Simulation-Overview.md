

Running an Aerie simulation requires a mission model that describes effects of activities over modeled resources, and a plan file that declares a schedule of activity instances with specified parameters.
A plan file must be created with respect to an mission model file, since activities in a plan and their parameters are validated against activity type definitions in the mission model.

Here's a summary workflow to getting a simulation result from Aerie.
1. Install Aerie services following instructions on [Product Guide](https://github.com/NASA-AMMOS/aerie/wiki/Product-Guide)
2. Create a Merlin mission model following the instructions on [Developing a Mission Model](https://github.com/NASA-AMMOS/aerie/wiki/Developing-a-Mission-Model) page.
3. Upload the mission model to Aerie through [Planning Web GUI](https://github.com/NASA-AMMOS/aerie/wiki/Planning-UI) or [Aerie API GraphQL graphical interface](https://github.com/NASA-AMMOS/aerie/wiki/Aerie-GraphQL-API-Software-Interface-Specification#graphql-playground).
4. Create a plan associated with the mission model using again the [Planning Web GUI](https://github.com/NASA-AMMOS/aerie/wiki/Planning-UI) or [Aerie API](https://github.com/NASA-AMMOS/aerie/wiki/Aerie-GraphQL-API-Software-Interface-Specification#graphql-playground)
5. Trigger simulation via either interfaces listed above.