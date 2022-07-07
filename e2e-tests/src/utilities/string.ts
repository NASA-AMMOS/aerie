//string formatting taken directly from: https://stackoverflow.com/questions/50428213/string-format-like-c-sharp-in-typescript
const StringFormat = (str: string, ...args: string[]) =>
    str.replace(/\[(\d+)\]/g, (match, index) => args[index] || '')

export default StringFormat;

// USAGE EXAMPLES:
/*
  let res = StringFormat("Hello {0}", "World!")
  console.log(res) // Hello World!
  res = StringFormat("Hello {0} {1}", "beautiful", "World!")
  console.log(res) // Hello beautiful World!
  res = StringFormat("Hello {0},{0}!", "beauty")
  console.log(res) //Hello beauty,beauty!
  res = StringFormat("Hello {0},{0}!", "beauty", "World!")
  console.log(res) //Hello beauty,beauty!
*/
