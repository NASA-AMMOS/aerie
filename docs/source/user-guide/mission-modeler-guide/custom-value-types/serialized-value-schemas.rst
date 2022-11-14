========================
Serialized Value Schemas
========================

Creating value schemas from JSON/GraphQL is a little less
straightforward, since your IDE won’t be able to help you, but fear not,
you’ve come to the right place. A value schema is created by declaring
an object with a ``type`` field that tells which type of schema is being
created. The values allowed in this field are given below: - ``"real"``
corresponds to ``REAL`` - ``"int"`` corresponds to ``INT`` -
``"boolean"`` corresponds to ``BOOLEAN`` - ``"string"`` corresponds to
``STRING`` - ``"duration"`` corresponds to ``DURATION`` - ``"path"``
corresponds to ``PATH`` - ``"variant"`` corresponds to ``VARIANT`` -
``"series"`` corresponds to ``SERIES`` - ``"struct"`` corresponds to
``STRUCT``

Variant
~~~~~~~

For the ``"variant"`` type, you’ll need to include a second field called
``variants`` whose value is a list of objects specifying the
string-valued ``key`` and ``label`` fields of each variant like this:

::

   {
     "type": "variant",
     "variants": [
       {
         "key": "ON",
         "label": "ON"
       },
       {
         "key": "OFF",
         "label": "OFF"
       }
     ]
   }

Series
~~~~~~

For the ``"series"`` type, a second field called ``"items"`` must be
included as well that provides the value schema for the items in the
series. See the below example, a value schema for a list of integers.

::

   {
     "type": "series",
     "items": {
       "type": "int"
     }
   }

Struct
~~~~~~

Lastly, for the ``"struct"`` type, a second field called ``"items"``
must be included that provides the actual structure of the struct,
mapping string keys to their corresponding value schema. See the below
example, a value schema for a struct with a string-valued ``label``
field, real-valued ``position`` field, and boolean-valued ``on`` field:

::

   {
     "type": "struct",
     "items": {
       "label": { "type": "string" },
       "position": { "type": "real" },
       "on": { "type": "boolean" }
     }
   }

Examples Creating Serialized Value Schemas
------------------------------------------

Below are more examples of creating serialized value schemas using JSON:

A value schema for an integer:

.. code:: json

   {
     "type": "int"
   }

A value schema for a list of paths:

.. code:: json

   {
     "type": "series",
     "items": {
       "type": "path"
     }
   }

A value schema for a list of lists of booleans:

.. code:: json

   {
     "type": "series",
     "items": {
       "type": "series",
       "items": {
         "type": "boolean"
       }
     }
   }

A value schema for a structure containing a list of integers labeled
``lints``, and a boolean labeled ``active``:

.. code:: json

   {
     "type": "struct",
     "items": {
       "lints": {
         "type": "series",
         "items": { "type": "int" }
       },
       "active": {
         "type": "boolean"
       }
     }
   }
