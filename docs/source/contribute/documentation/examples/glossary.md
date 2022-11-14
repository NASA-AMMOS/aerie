# Glossary

The [Aerie Glossary](../../../aerie-glossary.md) contains all the terms used throughout the Aerie Documentation.
They are broken up by subject. Terms need to be written with a hanging indent from the definition. 

Citing a term in the glossary uses the following syntax:

```{eval-rst}
.. tabs::

  .. code-tab:: rst
    
    This is how you write a :term:`Constraint`.
  
  .. code-tab:: md
    
    This is how you write a {term}`Constraint`.
```

which renders as:

This is how you write a {term}`Constraint`.

Terms must match _exactly_ with the spelling used in the glossary entry.
If you need to cite a term but cannot use the exact representation in the glossary 
(for example, using `Contraints` instead of `Constraint`), use the following syntax:

```{eval-rst}
.. tabs::

  .. group-tab:: reStructuredText
  
    .. code-block:: rst
    
      :term:`phrase <term as written in Glossary>`
      
    For example:
    
    .. code-block:: rst
    
      Aerie uses a :term:`Postgres <PostgreSQL>` database.  
      
  
  .. group-tab:: Markdown
  
    .. code-block:: md
    
      {term}`phrase <term as written in Glossary>`
      
    For example:
    
    .. code-block:: md
    
      Aerie uses a {term}`Postgres <PostgreSQL>` database.  
```
Which renders as:


Aerie uses a {term}`Postgres <PostgreSQL>` database.

## Abbreviations and Acronyms

Abbreviations and acronyms are defined in-line. The initial letters or abbreviation is first followed by the longer form.
```{eval-rst}
For example:

.. tabs:: 

  .. group-tab:: reStructuredText
  
    .. code-block:: rst
      
      :abbr:`API (Application Programming Interface)`
    
    or
    
    .. code-block:: rst
    
      :abbr:`Overwrite (Same data cells overwritten many times)`
  
  .. group-tab:: Markdown
  
    .. code-block:: md
      
      {abbr}`API (Application Programming Interface)`
    
    or
    
    .. code-block:: md
    
      {abbr}`Overwrite (Same data cells overwritten many times)`   
```

Which render as {abbr}`API (Application Programming Interface)` and {abbr}`Overwrite (Same data cells overwritten many times)`, respectively.
