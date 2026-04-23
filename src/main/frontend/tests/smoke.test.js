import { describe, it, expect } from 'vitest';
import { greet } from '../src/main.js';

describe('greet', () => {
  it('returns a greeting for the given name', () => {
    expect(greet('world')).toBe('Hello, world!');
  });
});
