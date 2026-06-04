import type { Config } from 'jest'

const config: Config = {
  testEnvironment: 'jsdom',
  transform: {
    '^.+\\.tsx?$': [
      'ts-jest',
      {
        tsconfig: {
          // ts-jest는 bundler 모듈 해석을 지원하지 않으므로 node로 오버라이드
          moduleResolution: 'node',
          module: 'commonjs',
          jsx: 'react-jsx',
        },
      },
    ],
  },
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/$1',
  },
  // Jest 프레임워크 설치 후 실행할 설정 파일 (jest-dom 커스텀 매처 등록)
  setupFilesAfterEnv: ['@testing-library/jest-dom'],
  testMatch: [
    '**/__tests__/**/*.test.ts',
    '**/__tests__/**/*.test.tsx',
  ],
}

export default config
