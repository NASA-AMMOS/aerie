export const hasClass = (targetElement: any, className: string) => {
  return targetElement.getAttribute('class').then((classes: string) => {
    return classes.split(' ').indexOf(className) !== -1;
  });
};
