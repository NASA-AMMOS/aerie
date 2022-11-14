# Code Blocks

Code blocks (also known as code fences) are for displaying code. By default, the code is copyable and includes a copy icon.
To keep code snippets neat, separate the display from the command.
This keeps the command copyable without having the screen return inside the command.

## Basic usage

You can set the syntax highlighting language of a code block to any supported by [Pygments](https://pygments.org/languages/). 
For example, to create a code block with Python syntax highlighting:

```{eval-rst}
.. tabs::
  
  .. group-tab:: reStructuredText
    
    .. note:: The blank line between ``.. code-block:: python`` and ``# This line prints "Hello World!"`` is significant.
    
    .. code-block:: rst
    
      .. code-block:: python
    
        # This line prints "Hello World!"
        print('Hello World!')
   
  
  .. group-tab:: Markdown
  
    .. code-block:: md
  
      ```{code-block} python
      # This line prints "Hello World!"
      print('Hello World!')
      ```
```

Renders as: 

```{code-block} python
# This line prints "Hello World!"
print('Hello World!')
```

```{tip}
To not include any syntax highlighting, set the language to `none`.
```

If you are including a large example (an entire file) as a code-block, refer to :doc:`Literal Include <includes>`.

## Hide copy button

Add the class ``hide-copy-button`` to the ``code-block`` directive to hide the copy button.

For example:

```{eval-rst}
.. tabs::
  
  .. group-tab:: reStructuredText
    
    .. code-block:: rst
    
      .. code-block:: python
        :class: hide-copy-button
        
        # This line prints "Hello World!"
        print('Hello World!')
    
    Renders as:
    
    .. code-block:: python
      :class: hide-copy-button
      
      # This line prints "Hello World!"
      print('Hello World!')
   
    For more information on what you can do with code blocks in RST, see `here <https://www.sphinx-doc.org/en/master/usage/restructuredtext/directives.html#directive-code-block>`__
  
  .. group-tab:: Markdown
  
    .. code-block:: md
    
      ```{code-block} python
      ---
      class: hide-copy-button
      ---
      # This line prints "Hello World!"
      print('Hello World!')
      ```
    
    Renders as:
    
    .. code-block:: python
      :class: hide-copy-button
      
      # This line prints "Hello World!"
      print('Hello World!')
      
    For more information on what you can do with code blocks in Markdown, see `here <https://myst-parser.readthedocs.io/en/latest/syntax/roles-and-directives.html?highlight=code%20blocks#code>`__
```
