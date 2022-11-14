# Lists

There are two kinds of lists, ordered and unordered.
Remember to insert a blank line before and after the lists.

``````{tabs}

`````{group-tab} reStructuredText

````{list-table}
---
widths: 25 25 25 25
header-rows: 1
---

* - List Type
  - Markup
  - Renders as
  - Description
* - Ordered List
  - ```{code-block} rst
    #. First Item
    #. Second Item
      
       a. Sub Item
       #. Sub Item
      
    #. Third Item
    ```
  - ```{eval-rst}
    1. First Item
    #. Second Item
       
       a. Sub Item
       #. Sub Item
       
    #. Third Item
    ```
  - Use this markup for procedures and in any place where order matters. Use number sign for the number. When the page is generated it is numbered automatically. A blank line is required at the end of a list.
* - Unordered List
  - ```{code-block} rst
    * First Item
    * Second Item
      
      * Sub Item
      * Sub Item
      
    * Third Item
    ```
  - ```{eval-rst}
    * First Item
    * Second Item
      
      * Sub Item
      * Sub Item
      
    * Third Item
    ```
  - Use for any list where order doesn't matter. Use an asterisk for the bullet. When the page is generated it is bulleted automatically. A blank line is required at the end of a list.

````

`````

`````{group-tab} Markdown

````{list-table}
---
widths: 25 25 25 25
header-rows: 1
---

* - List Type
  - Markup
  - Renders as
  - Description
* - Ordered List
  - ```{code-block} md
    1. First Item
    2. Second Item 
       1. Sub Item
       2. Sub Item
    3. Third Item
    ```
  - 1. First Item
    2. Second Item 
       1. Sub Item
       2. Sub Item
    3. Third Item
  - Use this markup for procedures and in any place where order matters. Use number sign for the number. When the page is generated it is numbered automatically. A blank line is required at the end of a list.
* - Unordered List
  - ```{code-block} md
    * First Item
    * Second Item 
      * Sub Item
      * Sub Item
    * Third Item
    ```
  - * First Item
    * Second Item 
      * Sub Item
      * Sub Item
    * Third Item
  - Use for any list where order doesn't matter. Use an asterisk for the bullet. When the page is generated it is bulleted automatically. A blank line is required at the end of a list.

````

`````

``````
