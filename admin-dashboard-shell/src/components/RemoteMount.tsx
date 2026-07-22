import React, { Suspense } from 'react';
import { RemoteErrorBoundary } from './RemoteErrorBoundary';
import { SectionSkeleton } from './SectionSkeleton';

interface RemoteMountProps {
    children: React.ReactNode;
}

export const RemoteMount: React.FC<RemoteMountProps> = ({ children }) => {
    return (
        <RemoteErrorBoundary>
            <Suspense fallback={<SectionSkeleton />}>
                {children}
            </Suspense>
        </RemoteErrorBoundary>
    );
};