# Tabs

When there are several options available and the reader will use one and keep using that option throughout the procedure,
a tabbed content box is the best way to display this information.

## Syntax

`````````{tabs}

````{code-tab} rst

.. tabs::

  .. group-tab:: Tab 1 Name
  
    Tab 1 Content
    
  .. group-tab:: Tab 2 Name
    
    Tab 2 Content
````


``````{code-tab} md

````{tabs}

```{group-tab} Tab 1 Name

Tab 1 Content
```

```{group-tab} Tab 2 Name

Tab 2 Content
```
````
``````

`````````

## Usage

```````{tabs}

````{code-tab} rst

.. tabs::
  
  .. group-tab:: MacOS (BSD sed)
    
    To replace every instance of the word "Hello" with the word "World" in a file using BSD sed, use the following command:
    
    .. code-block:: shell
      
      sed -i '' 's/Hello/World/g' index.rst

  .. group-tab:: Linux (GNU sed)
    
    To replace every instance of the word "Hello" with the word "World" in a file using GNU sed, use the following command:
    
    .. code-block:: shell
      
      sed -i -e 's/README/index/g' index.rst
  
  .. group-tab:: Windows (Powershell)
    
    Windows has no ``sed`` equivalent natively installed. Instead, in order to replace every instance of the word "Hello" with the word "World" in a file, use the following command in Powershell:
    
    .. code-block:: shell
      
      (Get-Content index.rst) -replace 'Hello', 'World' | Out-File -encoding ASCII index.rst

````


``````{code-tab} md

`````{tabs}

````{group-tab} MacOS (BSD sed)

To replace every instance of the word "Hello" with the word "World" in a file using BSD sed, use the following command:

```{code-block} shell
sed -i '' 's/Hello/World/g' index.rst
```
````

````{group-tab} Linux (GNU sed)

To replace every instance of the word "Hello" with the word "World" in a file using GNU sed, use the following command:

```{code-block} shell
sed -i -e 's/README/index/g' index.rst
```
````

````{group-tab} Windows (Powershell)

Windows has no ``sed`` equivalent natively installed. Instead, in order to replace every instance of the word "Hello" with the word "World" in a file, use the following command in Powershell:

```{code-block} shell
(Get-Content index.rst) -replace 'Hello', 'World' | Out-File -encoding ASCII index.rst
```
````
`````
``````
```````

Renders as:

`````{tabs}

````{group-tab} MacOS (BSD sed)

To replace every instance of the word "Hello" with the word "World" in a file using BSD sed, use the following command:

```{code-block} shell
sed -i '' 's/Hello/World/g' index.rst
```
````

````{group-tab} Linux (GNU sed)

To replace every instance of the word "Hello" with the word "World" in a file using GNU sed, use the following command:

```{code-block} shell
sed -i -e 's/README/index/g' index.rst
```
````

````{group-tab} Windows (Powershell)

Windows has no ``sed`` equivalent natively installed. Instead, in order to replace every instance of the word "Hello" with the word "World" in a file,
use the following command in Powershell:

```{code-block} shell
(Get-Content index.rst) -replace 'Hello', 'World' | Out-File -encoding ASCII index.rst
```
````

`````

## Code Tabs

When the only content of a tab will be a [code-block](./code-blocks.md), use `code-tab` instead of `group-tab`. 

### Syntax

``````{tabs}
```{code-tab} rst
.. code-tab:: language (Optional Displayed Name)
    
  ...code...
```

````{code-tab} md 

```{code-tab} language (Optional Displayed Name)
...code...
```
````
``````

If you exclude the Optional Displayed Name, then the proper name of the given language will be displayed.

### Usage

``````{tabs}
```{code-tab} rst

.. tabs::
  
  .. code-tab:: shell MacOS (BSD sed)
    
    sed -i '' 's/Hello/World/g' index.rst

  .. code-tab:: shell Linux (GNU sed)
    
    sed -i -e 's/README/index/g' index.rst

  .. code-tab:: shell Windows (Powershell)
    
    (Get-Content index.rst) -replace 'Hello', 'World' | Out-File -encoding ASCII index.rst

```

`````{code-tab} md

````{tabs}

```{code-tab} shell MacOS (BSD sed)
sed -i '' 's/Hello/World/g' index.rst
```

```{code-tab} shell Linux (GNU sed)
sed -i -e 's/README/index/g' index.rst
```

```{code-tab} shell Windows (Powershell)
(Get-Content index.rst) -replace 'Hello', 'World' | Out-File -encoding ASCII index.rst
```
````

`````
``````

Renders as:

````{tabs}

```{code-tab} shell MacOS (BSD sed)
sed -i '' 's/Hello/World/g' index.rst
```

```{code-tab} shell Linux (GNU sed)
sed -i -e 's/README/index/g' index.rst
```

```{code-tab} shell Windows (Powershell)
(Get-Content index.rst) -replace 'Hello', 'World' | Out-File -encoding ASCII index.rst
```
````
