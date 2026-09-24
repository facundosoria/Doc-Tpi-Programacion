// Catalogo de desafios del cuatrimestre (hardcoded para el workbench).
export interface ChallengeExercise {
  id: string;
  type: 'BE' | 'FE';
  name: string;
  title: string;
  tags: string[];
}

export const CHALLENGES: ChallengeExercise[] = [
  { id: 'be-01', type: 'BE', name: '01-hello-java', title: 'Hello Java', tags: ['java', 'intro', 'basics'] },
  { id: 'be-02', type: 'BE', name: '02-java-stdin-stdout', title: 'Java Stdin Stdout', tags: ['java', 'io', 'scanner'] },
  { id: 'be-03', type: 'BE', name: '03-java-stdin-stdout-2', title: 'Java Stdin Stdout 2', tags: ['java', 'io', 'scanner'] },
  { id: 'be-04', type: 'BE', name: '03-stdin-stdout-boolean', title: 'Stdin Stdout Boolean', tags: ['java', 'io', 'boolean', 'data-types'] },
  { id: 'be-05', type: 'BE', name: '03-stdin-stdout-byte', title: 'Stdin Stdout Byte', tags: ['java', 'io', 'byte', 'data-types'] },
  { id: 'be-06', type: 'BE', name: '03-stdin-stdout-int', title: 'Stdin Stdout Int', tags: ['java', 'io', 'int', 'data-types'] },
  { id: 'be-07', type: 'BE', name: '03-stdin-stdout-string', title: 'Stdin Stdout String', tags: ['java', 'io', 'string', 'data-types'] },
  { id: 'be-08', type: 'BE', name: '04-java-if-else', title: 'Java If Else', tags: ['java', 'conditionals', 'logic'] },
  { id: 'be-09', type: 'BE', name: '04-java-variables', title: 'Java Variables', tags: ['java', 'variables', 'basics'] },
  { id: 'be-10', type: 'BE', name: '05-java-loops', title: 'Java Loops', tags: ['java', 'loops', 'for', 'while'] },
  { id: 'be-11', type: 'BE', name: '06-java-loops-2', title: 'Java Loops 2', tags: ['java', 'loops', 'math'] },
  { id: 'be-12', type: 'BE', name: '07-java-substrings', title: 'Java Substrings', tags: ['java', 'strings', 'substring'] },
  { id: 'be-13', type: 'BE', name: '08-java-substrings-2', title: 'Java Substrings 2', tags: ['java', 'strings', 'compare'] },
  { id: 'be-14', type: 'BE', name: '09-java-strings', title: 'Java Strings', tags: ['java', 'strings'] },
  { id: 'be-15', type: 'BE', name: '10-java-strings-2', title: 'Java Strings 2', tags: ['java', 'strings'] },
  { id: 'be-16', type: 'BE', name: '11-java-strings-3', title: 'Java Strings 3', tags: ['java', 'strings', 'reverse'] },
  { id: 'be-17', type: 'BE', name: '12-list', title: 'List', tags: ['java', 'collections', 'list', 'arraylist'] },
  { id: 'be-18', type: 'BE', name: '13-map', title: 'Map', tags: ['java', 'collections', 'map', 'hashmap'] },
  { id: 'be-19', type: 'BE', name: '14-stack', title: 'Stack', tags: ['java', 'collections', 'stack', 'data-structures'] },
  { id: 'be-20', type: 'BE', name: '16-testing', title: 'Testing', tags: ['java', 'testing', 'junit', 'unit-tests'] },
  { id: 'be-21', type: 'BE', name: '17-mock', title: 'Mock', tags: ['java', 'testing', 'mockito', 'mocks'] },
  { id: 'be-22', type: 'BE', name: 'buscaminas', title: 'Buscaminas', tags: ['game', 'matrix', 'logic'] },
  { id: 'be-23', type: 'BE', name: 'challenge-01-queens-attack', title: 'Queens Attack', tags: ['challenge', 'chess', 'algorithms', 'matrix'] },
  { id: 'be-24', type: 'BE', name: 'challenge-02-guess-number', title: 'Guess Number', tags: ['challenge', 'game', 'binary-search'] },
  { id: 'be-25', type: 'BE', name: 'challenge-03-rain', title: 'Rain Trapping Water', tags: ['challenge', 'algorithms', 'two-pointers'] },
  { id: 'be-26', type: 'BE', name: 'challenge-04-islands', title: 'Islands', tags: ['challenge', 'dfs', 'bfs', 'matrix'] },
  { id: 'be-27', type: 'BE', name: 'dominio-elemental', title: 'Dominio Elemental', tags: ['oop', 'domain', 'architecture'] },
  { id: 'be-28', type: 'BE', name: 'dynamic-programming', title: 'Dynamic Programming', tags: ['algorithms', 'dp', 'optimization'] },
  { id: 'be-29', type: 'BE', name: 'maze', title: 'Maze', tags: ['algorithms', 'pathfinding', 'backtracking', 'matrix'] },
  { id: 'be-30', type: 'BE', name: 'secuencias-elementales', title: 'Secuencias Elementales', tags: ['algorithms', 'arrays', 'sequences'] },
  { id: 'be-31', type: 'BE', name: 'student-api', title: 'Student API', tags: ['api', 'rest', 'spring', 'backend'] },
  { id: 'fe-01', type: 'FE', name: 'exercise-adding-even-numbers', title: 'Adding Even Numbers', tags: ['math', 'loops', 'arrays'] },
  { id: 'fe-02', type: 'FE', name: 'exercise-anagram', title: 'Anagram', tags: ['strings', 'sorting', 'validation'] },
  { id: 'fe-03', type: 'FE', name: 'exercise-armstrong-number', title: 'Armstrong Number', tags: ['math', 'numbers'] },
  { id: 'fe-04', type: 'FE', name: 'exercise-ascending-order', title: 'Ascending Order', tags: ['sorting', 'arrays'] },
  { id: 'fe-05', type: 'FE', name: 'exercise-binary-to-decimal', title: 'Binary to Decimal', tags: ['conversion', 'math', 'binary'] },
  { id: 'fe-06', type: 'FE', name: 'exercise-checkers-game', title: 'Checkers Game', tags: ['game', 'board', 'ui'] },
  { id: 'fe-07', type: 'FE', name: 'exercise-descending-order', title: 'Descending Order', tags: ['sorting', 'arrays'] },
  { id: 'fe-08', type: 'FE', name: 'exercise-element-search', title: 'Element Search', tags: ['search', 'arrays'] },
  { id: 'fe-09', type: 'FE', name: 'exercise-factorial-number', title: 'Factorial Number', tags: ['math', 'recursion'] },
  { id: 'fe-10', type: 'FE', name: 'exercise-fibonacci', title: 'Fibonacci', tags: ['math', 'sequence', 'recursion'] },
  { id: 'fe-11', type: 'FE', name: 'exercise-fibonacci-sequence-generator', title: 'Fibonacci Sequence Generator', tags: ['math', 'sequence', 'generators'] },
  { id: 'fe-12', type: 'FE', name: 'exercise-form-validations-storage', title: 'Form Validations Storage', tags: ['forms', 'localstorage', 'validation'] },
  { id: 'fe-13', type: 'FE', name: 'exercise-hanged-man', title: 'Hanged Man', tags: ['game', 'ui', 'strings'] },
  { id: 'fe-14', type: 'FE', name: 'exercise-image-gallery', title: 'Image Gallery', tags: ['ui', 'dom', 'images'] },
  { id: 'fe-15', type: 'FE', name: 'exercise-longest-substring-without-repeats', title: 'Longest Substring Without Repeats', tags: ['strings', 'sliding-window', 'algorithms'] },
  { id: 'fe-16', type: 'FE', name: 'exercise-major-number-array', title: 'Major Number Array', tags: ['arrays', 'math', 'max'] },
  { id: 'fe-17', type: 'FE', name: 'exercise-minor-number-array', title: 'Minor Number Array', tags: ['arrays', 'math', 'min'] },
  { id: 'fe-18', type: 'FE', name: 'exercise-missing-number', title: 'Missing Number', tags: ['arrays', 'math', 'search'] },
  { id: 'fe-19', type: 'FE', name: 'exercise-naval-battle', title: 'Naval Battle', tags: ['game', 'matrix', 'board'] },
  { id: 'fe-20', type: 'FE', name: 'exercise-number-converter', title: 'Number Converter', tags: ['conversion', 'math'] },
  { id: 'fe-21', type: 'FE', name: 'exercise-palindrome-number', title: 'Palindrome Number', tags: ['math', 'numbers', 'validation'] },
  { id: 'fe-22', type: 'FE', name: 'exercise-real-time-chat', title: 'Real Time Chat', tags: ['chat', 'websocket', 'async'] },
  { id: 'fe-23', type: 'FE', name: 'exercise-roman-numerals-to-decimal', title: 'Roman Numerals to Decimal', tags: ['conversion', 'math', 'roman-numbers'] },
  { id: 'fe-24', type: 'FE', name: 'exercise-scientific-calculator', title: 'Scientific Calculator', tags: ['ui', 'calculator', 'math'] },
  { id: 'fe-25', type: 'FE', name: 'exercise-second-major', title: 'Second Major', tags: ['arrays', 'math', 'search'] },
  { id: 'fe-26', type: 'FE', name: 'exercise-sort-search-array', title: 'Sort Search Array', tags: ['sorting', 'search', 'arrays'] },
  { id: 'fe-27', type: 'FE', name: 'exercise-stopwatch', title: 'Stopwatch', tags: ['timer', 'ui', 'intervals'] },
  { id: 'fe-28', type: 'FE', name: 'exercise-sum-digits', title: 'Sum Digits', tags: ['math', 'numbers'] },
  { id: 'fe-29', type: 'FE', name: 'exercise-team-duel-simulation', title: 'Team Duel Simulation', tags: ['simulation', 'game', 'logic'] },
  { id: 'fe-30', type: 'FE', name: 'exercise-todo-list', title: 'Todo List', tags: ['crud', 'ui', 'storage'] },
  { id: 'fe-31', type: 'FE', name: 'exercise-unique-different-number', title: 'Unique Different Number', tags: ['arrays', 'sets', 'search'] },
  { id: 'fe-32', type: 'FE', name: 'mock-blackjack', title: 'Blackjack', tags: ['game', 'cards', 'logic'] },
  { id: 'fe-33', type: 'FE', name: 'mock-trick-game', title: 'Trick Game', tags: ['game', 'cards', 'truco'] },
  { id: 'fe-34', type: 'FE', name: 'ng-exercise-challenge-board', title: 'Challenge Board', tags: ['angular', 'board', 'ui'] },
];

const UUID_V5_NAMESPACE = '6ba7b810-9dad-11d1-80b4-00c04fd430c8';

/** FNV-1a 64-bit deterministic hash, split into 16 bytes to build a UUID-shaped value. */
function fnv1a(input: string): Uint8Array {
  let hash = 0xcbf29ce484222325n;
  const prime = 0x100000001b3n;
  for (let index = 0; index < input.length; index += 1) {
    hash ^= BigInt(input.charCodeAt(index));
    hash = (hash * prime) & 0xffffffffffffffffn;
  }
  const bytes = new Uint8Array(16);
  for (let index = 0; index < 16; index += 1) {
    bytes[index] = Number((hash >> BigInt((15 - index) * 8)) & 0xffn);
  }
  return bytes;
}

/** Deterministic UUID derived from the exercise id (stable across sessions and environments). */
export function challengeUuidV5(id: string): string {
  const bytes = fnv1a(`${UUID_V5_NAMESPACE}:${id}`);
  bytes[6] = (bytes[6] & 0x0f) | 0x50;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const b = (index: number) => bytes[index].toString(16).padStart(2, '0');
  return `${b(0)}${b(1)}${b(2)}${b(3)}-${b(4)}${b(5)}-${b(6)}${b(7)}-${b(8)}${b(9)}-${b(10)}${b(11)}${b(12)}${b(13)}${b(14)}${b(15)}`;
}
