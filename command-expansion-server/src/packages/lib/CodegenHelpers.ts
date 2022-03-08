export function globalDeclaration(declarations: string):string {
  return `declare global {\n${indent(declarations)}\n}`;
}

export function interfaceDeclaration(name: string,  interfaceContents: string): string {
  return `interface ${name} {\n${indent(interfaceContents)}\n}`;
}

export function indent(text:string): string {
  return text.split("\n").map(line => `\t${line}`).join("\n");
}
