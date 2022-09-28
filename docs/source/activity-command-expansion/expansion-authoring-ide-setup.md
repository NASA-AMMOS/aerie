# Expansion Authoring IDE Setup

Now it is time to start writing the expansion logic for the activity type. The recommended IDE is Visual Studio Code as we are looking at embedding this IDE as a web editor as a future service. Another option is IntelliJ but you will need the paid Ultimate version to use the typescript plugin.

## Recommended IDE:
* Visual Studio Code: https://code.visualstudio.com/
* InteliJ: https://www.jetbrains.com/idea/

## Setup:

1. Open a new project folder. You can create a new folder as your workspace
 
<img width="1792" alt="Screen Shot 2022-03-14 at 1 31 14 PM" src="https://user-images.githubusercontent.com/70245883/158256230-026205a7-4a3a-4727-80cd-867133fc3740.png">
 
 
2. Drag you <activity_library>.ts and <command_library>.ts into you project
 
 
<img width="1404" alt="Screen Shot 2022-03-14 at 1 35 00 PM" src="https://user-images.githubusercontent.com/70245883/158256720-02186497-83ef-4e83-9ea5-23f3103980e3.png">
 

3. Create an expansion logic file. You can name it whatever you like. `ex. BakeBananaBreadExpansionLogic.ts`
4. Add the following below which is a boilerplate template

```ts
// This is the entry point for the expansion - name it whatever you want, it just has to be the default export
export default function CommandExpansion(
  // Everything related to this activity
  props: {
    activityInstance: ActivityType;
  }
): ExpansionReturn {
  // Commands are fully strongly typed, and intellisense in the authoring editor will
  // guide users to write correct expansions
  return [
    // COMMAND_A(arg1)
    Subroutine(),
    // Commands without any arguments omit the ()
    // COMMAND_C
  ];
}

// You can break it into smaller logical/reusable chunks to make comprehension easier
function Subroutine() {
  return [
    //COMMAND_B(arg1,arg2)
  ];
}
```

## Writing
As you start typing in the editor, notice that each command is validated and type-checked. This means as you write out your logic, the editor will ensure all commands are valid, and that each argument meets the requirements. When you are happy with your logic save your works.

<img width="1297" alt="Screen Shot 2022-03-29 at 8 47 47 AM" src="https://user-images.githubusercontent.com/70245883/160652341-3a2fde89-dc21-4a1f-be70-03b0549d9f9b.png">
